package com.clothingshop.chat.service;

import com.clothingshop.chat.entity.ChatConversation;
import com.clothingshop.chat.entity.ChatMessage;
import com.clothingshop.chat.entity.ChatParticipant;
import com.clothingshop.chat.repository.ChatConversationRepository;
import com.clothingshop.chat.repository.ChatMessageRepository;
import com.clothingshop.chat.repository.ChatParticipantRepository;
import com.clothingshop.chat.service.BannedWordFilterService.FilterOutcome;
import com.example.clothingshop.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {

    public record SendMessageResult(
        boolean blocked,
        boolean masked,
        List<String> matchedWords,
        ChatMessage savedMessage,
        ChatConversation conversation
    ) {}

    @PersistenceContext
    private EntityManager entityManager;

    private final ChatConversationRepository conversationRepository;
    private final ChatParticipantRepository participantRepository;
    private final ChatMessageRepository messageRepository;
    private final BannedWordFilterService filterService;

    public ChatService(ChatConversationRepository conversationRepository,
                       ChatParticipantRepository participantRepository,
                       ChatMessageRepository messageRepository,
                       BannedWordFilterService filterService) {
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.messageRepository = messageRepository;
        this.filterService = filterService;
    }

    @Transactional
    public ChatConversation openOrGetCustomerConversation(Integer customerId, String customerDisplayName) {
        return conversationRepository
            .findFirstByCustomerIdAndStatusOrderByUpdatedAtDesc(customerId, "OPEN")
            .orElseGet(() -> {
                ChatConversation conv = new ChatConversation();
                conv.setCustomerId(customerId);
                conv.setTitle("H\u1ed7 tr\u1ee3 KH: " + customerDisplayName);
                conv.setStatus("OPEN");
                ChatConversation saved = conversationRepository.save(conv);

                ChatParticipant p = new ChatParticipant();
                p.setConversationId(saved.getId());
                p.setUserId(customerId);
                p.setRoleInChat("CUSTOMER");
                participantRepository.save(p);
                return saved;
            });
    }

    @Transactional
    public ChatConversation createInternalRoom(String title, Integer creatorId, String creatorRole) {
        if (!"STAFF".equals(creatorRole) && !"ADMIN".equals(creatorRole)) {
            throw new SecurityException("Ch\u1ec9 nh\u00e2n vi\u00ean ho\u1eb7c qu\u1ea3n tr\u1ecb vi\u00ean m\u1edbi \u0111\u01b0\u1ee3c t\u1ea1o k\u00eanh n\u1ed9i b\u1ed9");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("T\u00ean k\u00eanh n\u1ed9i b\u1ed9 kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1ed1ng");
        }
        String cleanTitle = title.trim();
        if (cleanTitle.length() > 120) {
            throw new IllegalArgumentException("T\u00ean k\u00eanh n\u1ed9i b\u1ed9 t\u1ed1i \u0111a 120 k\u00fd t\u1ef1");
        }

        ChatConversation room = new ChatConversation();
        room.setCustomerId(creatorId);
        room.setAssignedStaffId(creatorId);
        room.setTitle(cleanTitle);
        room.setStatus("INTERNAL");
        room.setCreatedAt(LocalDateTime.now());
        room.setUpdatedAt(LocalDateTime.now());
        ChatConversation saved = conversationRepository.save(room);

        ChatParticipant creatorPart = new ChatParticipant();
        creatorPart.setConversationId(saved.getId());
        creatorPart.setUserId(creatorId);
        creatorPart.setRoleInChat(creatorRole);
        creatorPart.setJoinedAt(LocalDateTime.now());
        participantRepository.save(creatorPart);

        return saved;
    }

    @Transactional
    public Map<String, Object> addMemberToInternalRoom(Long conversationId,
                                                       Integer targetUserId,
                                                       Integer requesterId,
                                                       String requesterRole) {
        ChatConversation conv = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new IllegalArgumentException("Kh\u00f4ng t\u00ecm th\u1ea5y k\u00eanh #" + conversationId));
        if (!"INTERNAL".equals(conv.getStatus())) {
            throw new IllegalArgumentException("Ch\u1ec9 c\u00f3 th\u1ec3 th\u00eam nh\u00e2n vi\u00ean v\u00e0o k\u00eanh n\u1ed9i b\u1ed9");
        }
        validateAccess(conv, requesterId, requesterRole);

        if (participantRepository.findByConversationIdAndUserId(conversationId, targetUserId).isPresent()) {
            throw new IllegalArgumentException("Nh\u00e2n vi\u00ean n\u00e0y \u0111\u00e3 c\u00f3 m\u1eb7t trong k\u00eanh");
        }

        String targetRole = "STAFF";
        String targetEmail = "User #" + targetUserId;
        String targetName = targetEmail;
        if (entityManager != null) {
            List<User> found = entityManager.createQuery(
                    "SELECT u FROM User u JOIN FETCH u.role WHERE u.userId = :uid", User.class)
                .setParameter("uid", targetUserId)
                .setMaxResults(1)
                .getResultList();
            if (found.isEmpty()) {
                throw new IllegalArgumentException("Kh\u00f4ng t\u00ecm th\u1ea5y t\u00e0i kho\u1ea3n nh\u00e2n vi\u00ean #" + targetUserId);
            }
            User u = found.get(0);
            if (!Boolean.TRUE.equals(u.getIsActive())) {
                throw new IllegalArgumentException("T\u00e0i kho\u1ea3n nh\u00e2n vi\u00ean \u0111ang b\u1ecb kh\u00f3a");
            }
            targetRole = u.getRole().getRoleName().toUpperCase();
            if (!"STAFF".equals(targetRole) && !"ADMIN".equals(targetRole)) {
                throw new IllegalArgumentException("Ch\u1ec9 \u0111\u01b0\u1ee3c th\u00eam t\u00e0i kho\u1ea3n STAFF ho\u1eb7c ADMIN v\u00e0o k\u00eanh n\u1ed9i b\u1ed9");
            }
            targetEmail = u.getEmail();
            targetName = u.getFullName() != null ? u.getFullName() : targetEmail;
        }

        ChatParticipant part = new ChatParticipant();
        part.setConversationId(conversationId);
        part.setUserId(targetUserId);
        part.setRoleInChat(targetRole);
        part.setJoinedAt(LocalDateTime.now());
        participantRepository.save(part);

        conv.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conv);

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("conversationId", conversationId);
        info.put("userId", targetUserId);
        info.put("email", targetEmail);
        info.put("fullName", targetName);
        info.put("role", targetRole);
        return info;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listStaffUsers(Long conversationId) {
        Set<Integer> existingMemberIds = conversationId != null
            ? participantRepository.findByConversationId(conversationId).stream()
                .map(ChatParticipant::getUserId)
                .collect(Collectors.toSet())
            : Set.of();

        if (entityManager == null) {
            return List.of();
        }
        List<User> staffList = entityManager.createQuery(
                "SELECT u FROM User u JOIN FETCH u.role r WHERE r.roleName IN ('STAFF', 'ADMIN') AND u.isActive = true ORDER BY r.roleName, u.fullName",
                User.class)
            .getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : staffList) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("userId", u.getUserId());
            row.put("email", u.getEmail());
            row.put("fullName", u.getFullName() != null ? u.getFullName() : u.getEmail());
            row.put("role", u.getRole().getRoleName().toUpperCase());
            row.put("joined", existingMemberIds.contains(u.getUserId()));
            result.add(row);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listInternalMembers(Long conversationId, Integer requesterId, String requesterRole) {
        ChatConversation conv = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new IllegalArgumentException("Kh\u00f4ng t\u00ecm th\u1ea5y k\u00eanh #" + conversationId));
        validateAccess(conv, requesterId, requesterRole);

        List<ChatParticipant> parts = participantRepository.findByConversationId(conversationId);
        Map<Integer, User> userMap = new HashMap<>();
        if (entityManager != null && !parts.isEmpty()) {
            List<Integer> ids = parts.stream().map(ChatParticipant::getUserId).toList();
            List<User> users = entityManager.createQuery(
                    "SELECT u FROM User u JOIN FETCH u.role WHERE u.userId IN :ids", User.class)
                .setParameter("ids", ids)
                .getResultList();
            for (User u : users) {
                userMap.put(u.getUserId(), u);
            }
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (ChatParticipant p : parts) {
            User u = userMap.get(p.getUserId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", p.getUserId());
            item.put("roleInChat", p.getRoleInChat());
            item.put("email", u != null ? u.getEmail() : "User #" + p.getUserId());
            item.put("fullName", u != null && u.getFullName() != null ? u.getFullName() : ("User #" + p.getUserId()));
            list.add(item);
        }
        return list;
    }

    @Transactional(readOnly = true)
    public String resolveUserDisplayName(Integer userId) {
        if (userId == null || entityManager == null) return null;
        List<User> found = entityManager.createQuery(
                "SELECT u FROM User u WHERE u.userId = :uid", User.class)
            .setParameter("uid", userId)
            .setMaxResults(1)
            .getResultList();
        if (found.isEmpty()) return "User #" + userId;
        User u = found.get(0);
        return (u.getFullName() != null && !u.getFullName().isBlank()) ? u.getFullName() : u.getEmail();
    }

    @Transactional(readOnly = true)
    public Set<Integer> getConversationParticipantIds(Long conversationId) {
        return participantRepository.findByConversationId(conversationId).stream()
            .map(ChatParticipant::getUserId)
            .collect(Collectors.toSet());
    }

    @Transactional
    public ChatConversation claimConversation(Long conversationId, Integer staffId, String role) {
        ChatConversation conv = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new IllegalArgumentException("Kh\u00f4ng t\u00ecm th\u1ea5y h\u1ed9i tho\u1ea1i #" + conversationId));
        if ("CLOSED".equals(conv.getStatus())) {
            throw new IllegalArgumentException("H\u1ed9i tho\u1ea1i #" + conversationId + " \u0111\u00e3 \u0111\u00f3ng");
        }
        if ("INTERNAL".equals(conv.getStatus())) {
            throw new IllegalArgumentException("K\u00eanh n\u1ed9i b\u1ed9 kh\u00f4ng d\u00f9ng ch\u1ee9c n\u0103ng nh\u1eadn h\u1ed7 tr\u1ee3 kh\u00e1ch h\u00e0ng");
        }
        conv.setAssignedStaffId(staffId);
        conv.setUpdatedAt(LocalDateTime.now());
        if (participantRepository.findByConversationIdAndUserId(conversationId, staffId).isEmpty()) {
            ChatParticipant p = new ChatParticipant();
            p.setConversationId(conversationId);
            p.setUserId(staffId);
            p.setRoleInChat(role);
            participantRepository.save(p);
        }
        return conversationRepository.save(conv);
    }

    @Transactional
    public ChatConversation closeConversation(Long conversationId, Integer staffId, String role) {
        if (!"STAFF".equals(role) && !"ADMIN".equals(role)) {
            throw new SecurityException("Ch\u1ec9 nh\u00e2n vi\u00ean ho\u1eb7c qu\u1ea3n tr\u1ecb vi\u00ean m\u1edbi \u0111\u01b0\u1ee3c \u0111\u00f3ng h\u1ed9i tho\u1ea1i");
        }
        ChatConversation conv = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new IllegalArgumentException("Kh\u00f4ng t\u00ecm th\u1ea5y h\u1ed9i tho\u1ea1i #" + conversationId));
        if ("INTERNAL".equals(conv.getStatus())) {
            throw new IllegalArgumentException("Kh\u00f4ng th\u1ec3 \u0111\u00f3ng k\u00eanh n\u1ed9i b\u1ed9 nh\u00e2n vi\u00ean");
        }
        conv.setStatus("CLOSED");
        if (conv.getAssignedStaffId() == null) {
            conv.setAssignedStaffId(staffId);
        }
        conv.setUpdatedAt(LocalDateTime.now());
        return conversationRepository.save(conv);
    }

    @Transactional
    public void deleteConversation(Long conversationId) {
        ChatConversation conv = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new IllegalArgumentException("Kh\u00f4ng t\u00ecm th\u1ea5y h\u1ed9i tho\u1ea1i #" + conversationId));
        if (entityManager != null) {
            entityManager.createQuery("DELETE FROM ChatViolation v WHERE v.conversationId = :cid")
                .setParameter("cid", conversationId)
                .executeUpdate();
            entityManager.createQuery("DELETE FROM ChatMessage m WHERE m.conversationId = :cid")
                .setParameter("cid", conversationId)
                .executeUpdate();
            entityManager.createQuery("DELETE FROM ChatParticipant p WHERE p.conversationId = :cid")
                .setParameter("cid", conversationId)
                .executeUpdate();
        }
        conversationRepository.delete(conv);
    }

    @Transactional
    public ChatConversation openOrGetInternalStaffRoom(Integer staffId) {
        return conversationRepository.findAllByOrderByUpdatedAtDesc().stream()
            .filter(c -> "INTERNAL".equals(c.getStatus()))
            .filter(c -> participantRepository.findByConversationIdAndUserId(c.getId(), staffId).isPresent())
            .findFirst()
            .orElseGet(() -> createInternalRoom("K\u00eanh n\u1ed9i b\u1ed9 Nh\u00e2n vi\u00ean", staffId, "STAFF"));
    }

    @Transactional(readOnly = true)
    public List<ChatConversation> listConversationsForUser(Integer userId, String role) {
        return conversationRepository.findAllByOrderByUpdatedAtDesc().stream()
            .filter(c -> {
                if ("INTERNAL".equals(c.getStatus())) {
                    return userId != null && participantRepository.findByConversationIdAndUserId(c.getId(), userId).isPresent();
                }
                return "ADMIN".equals(role) || "STAFF".equals(role) || java.util.Objects.equals(c.getCustomerId(), userId);
            })
            .sorted(Comparator.comparingInt((ChatConversation c) -> "CLOSED".equals(c.getStatus()) ? 1 : 0))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getConversationMessages(Long conversationId, Integer requesterId, String requesterRole) {
        ChatConversation conv = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new IllegalArgumentException("Kh\u00f4ng t\u00ecm th\u1ea5y cu\u1ed9c h\u1ed9i tho\u1ea1i #" + conversationId));
        validateAccess(conv, requesterId, requesterRole);
        return messageRepository.findTop100ByConversationIdOrderByIdAsc(conversationId);
    }

    @Transactional
    public SendMessageResult sendMessage(Long conversationId, Integer senderId, String senderRole, String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            throw new IllegalArgumentException("N\u1ed9i dung tin nh\u1eafn kh\u00f4ng \u0111\u01b0\u1ee3c r\u1ed7ng");
        }
        if (rawContent.length() > 1800) {
            throw new IllegalArgumentException("Tin nh\u1eafn v\u01b0\u1ee3t qu\u00e1 1800 k\u00fd t\u1ef1");
        }

        ChatConversation conv = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new IllegalArgumentException("Kh\u00f4ng t\u00ecm th\u1ea5y cu\u1ed9c h\u1ed9i tho\u1ea1i #" + conversationId));
        validateAccess(conv, senderId, senderRole);

        if ("CLOSED".equals(conv.getStatus())) {
            throw new IllegalArgumentException("Cu\u1ed9c h\u1ed9i tho\u1ea1i \u0111\u00e3 \u0111\u01b0\u1ee3c \u0111\u00f3ng, vui l\u00f2ng m\u1edf h\u1ed9i tho\u1ea1i m\u1edbi");
        }

        if (!"INTERNAL".equals(conv.getStatus())
                && ("STAFF".equals(senderRole) || "ADMIN".equals(senderRole))
                && participantRepository.findByConversationIdAndUserId(conversationId, senderId).isEmpty()) {
            ChatParticipant staffPart = new ChatParticipant();
            staffPart.setConversationId(conversationId);
            staffPart.setUserId(senderId);
            staffPart.setRoleInChat(senderRole);
            participantRepository.save(staffPart);
            if (conv.getAssignedStaffId() == null) {
                conv.setAssignedStaffId(senderId);
            }
        }

        FilterOutcome outcome = filterService.evaluateAndLog(senderId, conversationId, rawContent.trim());
        if (outcome.blocked()) {
            return new SendMessageResult(true, false, outcome.matchedWords(), null, conv);
        }

        ChatMessage msg = new ChatMessage();
        msg.setConversationId(conversationId);
        msg.setSenderId(senderId);
        msg.setOriginalContent(rawContent.trim());
        msg.setFilteredContent(outcome.filteredText());
        msg.setMasked(outcome.masked());
        ChatMessage saved = messageRepository.save(msg);

        conv.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conv);

        return new SendMessageResult(false, outcome.masked(), outcome.matchedWords(), saved, conv);
    }

    private void validateAccess(ChatConversation conv, Integer userId, String role) {
        if ("INTERNAL".equals(conv.getStatus())) {
            if (!"ADMIN".equals(role) && !"STAFF".equals(role)) {
                throw new SecurityException("K\u00eanh n\u1ed9i b\u1ed9 ch\u1ec9 d\u00e0nh cho nh\u00e2n vi\u00ean");
            }
            if (userId == null || participantRepository.findByConversationIdAndUserId(conv.getId(), userId).isEmpty()) {
                throw new SecurityException("B\u1ea1n kh\u00f4ng c\u00f3 trong danh s\u00e1ch th\u00e0nh vi\u00ean c\u1ee7a k\u00eanh n\u1ed9i b\u1ed9 n\u00e0y");
            }
            return;
        }
        if ("ADMIN".equals(role) || "STAFF".equals(role)) {
            return;
        }
        if (!conv.getCustomerId().equals(userId)) {
            throw new SecurityException("B\u1ea1n kh\u00f4ng c\u00f3 quy\u1ec1n truy c\u1eadp cu\u1ed9c h\u1ed9i tho\u1ea1i n\u00e0y");
        }
    }
}