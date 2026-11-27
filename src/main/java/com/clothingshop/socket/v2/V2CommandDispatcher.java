package com.clothingshop.socket.v2;

import com.clothingshop.chat.entity.BannedWord;
import com.clothingshop.chat.entity.BannedWord.FilterAction;
import com.clothingshop.chat.entity.ChatConversation;
import com.clothingshop.chat.entity.ChatMessage;
import com.clothingshop.chat.service.BannedWordFilterService;
import com.clothingshop.chat.service.ChatService;
import com.clothingshop.chat.service.ChatService.SendMessageResult;
import com.clothingshop.chat.service.RealtimeCatalogService;
import com.example.clothingshop.entity.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class V2CommandDispatcher {

    public static final String INTERNAL_BRIDGE_SECRET = java.util.UUID.randomUUID().toString();

    @PersistenceContext
    private EntityManager entityManager;

    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final ChatService chatService;
    private final BannedWordFilterService bannedWordFilterService;
    private final RealtimeCatalogService catalogService;
    private final RealtimeSessionRegistry sessionRegistry;

    public V2CommandDispatcher(ObjectMapper objectMapper,
                               PasswordEncoder passwordEncoder,
                               ChatService chatService,
                               BannedWordFilterService bannedWordFilterService,
                               RealtimeCatalogService catalogService,
                               RealtimeSessionRegistry sessionRegistry) {
        this.objectMapper = objectMapper;
        this.passwordEncoder = passwordEncoder;
        this.chatService = chatService;
        this.bannedWordFilterService = bannedWordFilterService;
        this.catalogService = catalogService;
        this.sessionRegistry = sessionRegistry;
    }

    @SuppressWarnings("unchecked")
    public String dispatch(String rawJsonLine, ChatClientSession session) {
        String requestId = null;
        String type = "UNKNOWN";
        try {
            Map<String, Object> req = objectMapper.readValue(rawJsonLine, new TypeReference<>() {});
            requestId = req.get("requestId") != null ? String.valueOf(req.get("requestId")) : null;
            type = req.get("type") != null ? String.valueOf(req.get("type")).toUpperCase() : "UNKNOWN";
            Map<String, Object> payload = req.get("payload") instanceof Map<?, ?> m
                    ? (Map<String, Object>) m : Map.of();

            return switch (type) {
                case "PING" -> okResponse(requestId, "PONG", Map.of(
                    "handledBy", Thread.currentThread().getName(),
                    "activeSessions", sessionRegistry.activeCount()
                ));
                case "AUTH", "LOGIN" -> handleAuth(requestId, payload, session);
                case "LIST_PRODUCTS", "PRODUCT_LIST" -> {
                    requireAuth(session);
                    String keyword = str(payload.get("keyword"));
                    yield okResponse(requestId, "PRODUCTS_LIST", Map.of(
                        "items", catalogService.listCatalogSnapshot(keyword),
                        "handledBy", Thread.currentThread().getName()
                    ));
                }
                case "UPDATE_PRODUCT", "PRODUCT_UPDATE" -> handleUpdateProduct(requestId, payload, session);
                case "UPDATE_STOCK", "STOCK_ADJUST" -> handleStockAdjust(requestId, payload, session);
                case "LIST_STOCK_HISTORY" -> handleListStockHistory(requestId, payload, session);
                case "OPEN_CHAT" -> handleOpenChat(requestId, session);
                case "OPEN_INTERNAL_CHAT" -> handleOpenInternalChat(requestId, session);
                case "CREATE_INTERNAL_CHAT" -> handleCreateInternalChat(requestId, payload, session);
                case "ADD_INTERNAL_MEMBER" -> handleAddInternalMember(requestId, payload, session);
                case "LIST_STAFF_USERS" -> handleListStaffUsers(requestId, payload, session);
                case "LIST_INTERNAL_MEMBERS" -> handleListInternalMembers(requestId, payload, session);
                case "CLAIM_CHAT" -> handleClaimChat(requestId, payload, session);
                case "CLOSE_CONVERSATION" -> handleCloseConversation(requestId, payload, session);
                case "DELETE_CONVERSATION" -> handleDeleteConversation(requestId, payload, session);
                case "LIST_CONVERSATIONS" -> handleListConversations(requestId, session);
                case "GET_MESSAGES" -> handleGetMessages(requestId, payload, session);
                case "SEND_CHAT" -> handleSendChat(requestId, payload, session);
                case "LIST_BANNED_WORDS" -> {
                    requireRole(session, "ADMIN");
                    yield okResponse(requestId, "BANNED_WORDS_LIST", Map.of(
                        "words", bannedWordFilterService.listAll(),
                        "violations", bannedWordFilterService.listRecentViolations(),
                        "handledBy", Thread.currentThread().getName()
                    ));
                }
                case "ADD_BANNED_WORD" -> handleAddBannedWord(requestId, payload, session);
                case "DELETE_BANNED_WORD" -> handleDeleteBannedWord(requestId, payload, session);
                default -> errorResponse(requestId, "UNKNOWN_COMMAND", "L\u1ec7nh v2 kh\u00f4ng \u0111\u01b0\u1ee3c h\u1ed7 tr\u1ee3: " + type);
            };
        } catch (ObjectOptimisticLockingFailureException ex) {
            return errorResponse(requestId, "OPTIMISTIC_LOCK_CONFLICT",
                "S\u1ea3n ph\u1ea9m \u0111\u00e3 \u0111\u01b0\u1ee3c ng\u01b0\u1eddi kh\u00e1c s\u1eeda, b\u1ea5m T\u1ea3i l\u1ea1i");
        } catch (SecurityException ex) {
            return errorResponse(requestId, "FORBIDDEN", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            return errorResponse(requestId, "BAD_REQUEST", ex.getMessage());
        } catch (Exception ex) {
            return errorResponse(requestId, "SERVER_ERROR", "L\u1ed7i x\u1eed l\u00fd g\u00f3i tin (" + type + "): " + ex.getMessage());
        }
    }

    private String handleAuth(String requestId, Map<String, Object> payload, ChatClientSession session) {
        String loginId = str(payload.get("username"));
        if (loginId == null) loginId = str(payload.get("email"));
        String bridgeSecret = str(payload.get("bridgeSecret"));
        boolean trustedBridge = bridgeSecret != null && INTERNAL_BRIDGE_SECRET.equals(bridgeSecret);
        String password = str(payload.get("password"));
        if (loginId == null || (!trustedBridge && password == null)) {
            throw new IllegalArgumentException("Thi\u1ebfu t\u00e0i kho\u1ea3n ho\u1eb7c m\u1eadt kh\u1ea9u");
        }
        List<User> users = entityManager.createQuery(
                "SELECT u FROM User u JOIN FETCH u.role WHERE LOWER(u.email) = LOWER(:id) OR LOWER(u.fullName) = LOWER(:id)", User.class)
            .setParameter("id", loginId.trim())
            .setMaxResults(1)
            .getResultList();

        if (users.isEmpty()) {
            throw new SecurityException("Sai t\u00e0i kho\u1ea3n ho\u1eb7c m\u1eadt kh\u1ea9u");
        }
        User user = users.get(0);
        if (!Boolean.TRUE.equals(user.getIsActive()) || (!trustedBridge && !passwordEncoder.matches(password, user.getPasswordHash()))) {
            throw new SecurityException("T\u00e0i kho\u1ea3n b\u1ecb kh\u00f3a ho\u1eb7c sai m\u1eadt kh\u1ea9u");
        }
        String roleName = user.getRole().getRoleName().toUpperCase();
        String principalName = user.getEmail();
        session.authenticate(user.getUserId(), principalName, roleName);

        return okResponse(requestId, "AUTH_OK", Map.of(
            "userId", user.getUserId(),
            "username", principalName,
            "fullName", user.getFullName() != null ? user.getFullName() : principalName,
            "role", roleName,
            "handledBy", Thread.currentThread().getName()
        ));
    }

    private String handleUpdateProduct(String requestId, Map<String, Object> payload, ChatClientSession session) {
        requireRole(session, "STAFF", "ADMIN");
        Integer productId = intVal(payload.get("productId"));
        String name = str(payload.get("productName"));
        BigDecimal price = payload.get("basePrice") != null ? new BigDecimal(String.valueOf(payload.get("basePrice"))) : null;
        String description = str(payload.get("description"));
        Boolean active = payload.get("active") != null ? Boolean.parseBoolean(String.valueOf(payload.get("active"))) : null;
        Long expectedVersion = payload.get("productVersion") != null ? Long.parseLong(String.valueOf(payload.get("productVersion"))) : null;

        Map<String, Object> updated = catalogService.updateProductOptimistic(productId, name, price, description, active, expectedVersion);
        updated.put("handledBy", Thread.currentThread().getName());
        sessionRegistry.broadcastEvent("PRODUCT_UPDATED", updated);
        return okResponse(requestId, "PRODUCT_UPDATE_OK", updated);
    }

    private String handleListStockHistory(String requestId, Map<String, Object> payload, ChatClientSession session) {
        requireRole(session, "STAFF", "ADMIN");
        String sku = str(payload.get("sku"));
        List<Map<String, Object>> history = catalogService.listStockHistory(sku);
        return okResponse(requestId, "STOCK_HISTORY_LIST", Map.of(
            "history", history,
            "handledBy", Thread.currentThread().getName()
        ));
    }

    private String handleStockAdjust(String requestId, Map<String, Object> payload, ChatClientSession session) {
        requireRole(session, "STAFF", "ADMIN");
        String sku = str(payload.get("sku"));
        Integer delta = payload.get("delta") != null ? intVal(payload.get("delta")) : null;
        Integer quantity = payload.get("quantity") != null ? intVal(payload.get("quantity")) : null;
        String reason = str(payload.get("reason"));

        Map<String, Object> updated = catalogService.updateStockPessimistic(sku, delta, quantity, reason, session.getUsername());
        updated.put("handledBy", Thread.currentThread().getName());
        sessionRegistry.broadcastEvent("STOCK_UPDATED", updated);
        return okResponse(requestId, "STOCK_UPDATE_OK", updated);
    }

    private String handleOpenChat(String requestId, ChatClientSession session) {
        requireAuth(session);
        ChatConversation conv = chatService.openOrGetCustomerConversation(session.getUserId(), session.getUsername());
        List<ChatMessage> msgs = chatService.getConversationMessages(conv.getId(), session.getUserId(), session.getRole());
        Map<Integer, String> nameCache = new java.util.HashMap<>();
        List<Map<String, Object>> enriched = new java.util.ArrayList<>();
        for (ChatMessage msg : msgs) {
            Integer sid = msg.getSenderId();
            String senderName = nameCache.computeIfAbsent(sid, id -> {
                String resolved = chatService.resolveUserDisplayName(id);
                return (resolved != null && !resolved.isBlank()) ? resolved : ("User #" + id);
            });
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("id", msg.getId());
            item.put("messageId", msg.getId());
            item.put("conversationId", msg.getConversationId());
            item.put("senderId", sid);
            item.put("senderName", senderName);
            item.put("filteredContent", msg.getFilteredContent());
            item.put("createdAt", msg.getCreatedAt() != null ? msg.getCreatedAt().toString() : "");
            enriched.add(item);
        }
        String assignedName = conv.getAssignedStaffId() != null
            ? chatService.resolveUserDisplayName(conv.getAssignedStaffId())
            : "";
        Map<String, Object> resPayload = new java.util.LinkedHashMap<>();
        resPayload.put("conversation", conv);
        resPayload.put("assignedStaffName", assignedName != null ? assignedName : "");
        resPayload.put("messages", enriched);
        resPayload.put("handledBy", Thread.currentThread().getName());
        return okResponse(requestId, "CHAT_OPENED", resPayload);
    }

    private String handleOpenInternalChat(String requestId, ChatClientSession session) {
        requireRole(session, "STAFF", "ADMIN");
        ChatConversation internal = chatService.openOrGetInternalStaffRoom(session.getUserId());
        List<ChatMessage> history = chatService.getConversationMessages(internal.getId(), session.getUserId(), session.getRole());
        return okResponse(requestId, "INTERNAL_CHAT_OPENED", Map.of(
            "conversation", internal,
            "messages", history,
            "handledBy", Thread.currentThread().getName()
        ));
    }

    private String handleCreateInternalChat(String requestId, Map<String, Object> payload, ChatClientSession session) {
        requireRole(session, "STAFF", "ADMIN");
        String title = str(payload.get("title"));
        ChatConversation room = chatService.createInternalRoom(title, session.getUserId(), session.getRole());
        return okResponse(requestId, "INTERNAL_CHAT_CREATED", Map.of(
            "conversation", room,
            "handledBy", Thread.currentThread().getName()
        ));
    }

    private String handleAddInternalMember(String requestId, Map<String, Object> payload, ChatClientSession session) {
        requireRole(session, "STAFF", "ADMIN");
        if (payload.get("conversationId") == null || payload.get("userId") == null) {
            throw new IllegalArgumentException("Thi\u1ebfu conversationId ho\u1eb7c userId");
        }
        Long convId = Long.parseLong(String.valueOf(payload.get("conversationId")));
        Integer targetUserId = Integer.parseInt(String.valueOf(payload.get("userId")));
        Map<String, Object> added = chatService.addMemberToInternalRoom(convId, targetUserId, session.getUserId(), session.getRole());
        added.put("handledBy", Thread.currentThread().getName());
        sessionRegistry.broadcastEvent("INTERNAL_MEMBER_ADDED", added);
        return okResponse(requestId, "INTERNAL_MEMBER_ADDED", added);
    }

    private String handleListStaffUsers(String requestId, Map<String, Object> payload, ChatClientSession session) {
        requireRole(session, "STAFF", "ADMIN");
        Long convId = payload.get("conversationId") != null ? Long.parseLong(String.valueOf(payload.get("conversationId"))) : null;
        return okResponse(requestId, "STAFF_USERS_LIST", Map.of(
            "staffUsers", chatService.listStaffUsers(convId),
            "handledBy", Thread.currentThread().getName()
        ));
    }

    private String handleListInternalMembers(String requestId, Map<String, Object> payload, ChatClientSession session) {
        requireRole(session, "STAFF", "ADMIN");
        if (payload.get("conversationId") == null) {
            throw new IllegalArgumentException("Thi\u1ebfu conversationId");
        }
        Long convId = Long.parseLong(String.valueOf(payload.get("conversationId")));
        return okResponse(requestId, "INTERNAL_MEMBERS_LIST", Map.of(
            "conversationId", convId,
            "members", chatService.listInternalMembers(convId, session.getUserId(), session.getRole()),
            "handledBy", Thread.currentThread().getName()
        ));
    }

    private String handleClaimChat(String requestId, Map<String, Object> payload, ChatClientSession session) {
        requireRole(session, "STAFF", "ADMIN");
        Long convId = Long.parseLong(String.valueOf(payload.get("conversationId")));
        ChatConversation claimed = chatService.claimConversation(convId, session.getUserId(), session.getRole());
        Map<String, Object> info = Map.of(
            "conversationId", claimed.getId(),
            "assignedStaffId", session.getUserId(),
            "assignedStaffName", session.getUsername(),
            "handledBy", Thread.currentThread().getName()
        );
        sessionRegistry.broadcastEvent("CONVERSATION_CLAIMED", info);
        return okResponse(requestId, "CHAT_CLAIMED", info);
    }

    private String handleCloseConversation(String requestId, Map<String, Object> payload, ChatClientSession session) {
        requireRole(session, "STAFF", "ADMIN");
        if (payload.get("conversationId") == null) {
            throw new IllegalArgumentException("Thi\u1ebfu conversationId");
        }
        Long convId = Long.parseLong(String.valueOf(payload.get("conversationId")));
        ChatConversation closed = chatService.closeConversation(convId, session.getUserId(), session.getRole());
        Map<String, Object> info = Map.of(
            "conversationId", closed.getId(),
            "status", closed.getStatus(),
            "closedBy", session.getUsername(),
            "handledBy", Thread.currentThread().getName()
        );
        sessionRegistry.broadcastEvent("CONVERSATION_CLOSED", info);
        return okResponse(requestId, "CONVERSATION_CLOSED", info);
    }

    private String handleDeleteConversation(String requestId, Map<String, Object> payload, ChatClientSession session) {
        requireRole(session, "ADMIN");
        if (payload.get("conversationId") == null) {
            throw new IllegalArgumentException("Thi\u1ebfu conversationId");
        }
        Long convId = Long.parseLong(String.valueOf(payload.get("conversationId")));
        chatService.deleteConversation(convId);
        Map<String, Object> info = Map.of(
            "conversationId", convId,
            "deletedBy", session.getUsername(),
            "handledBy", Thread.currentThread().getName()
        );
        sessionRegistry.broadcastEvent("CONVERSATION_DELETED", info);
        return okResponse(requestId, "CONVERSATION_DELETED", info);
    }

    private String handleListConversations(String requestId, ChatClientSession session) {
        requireRole(session, "STAFF", "ADMIN");
        return okResponse(requestId, "CONVERSATIONS_LIST", Map.of(
            "conversations", chatService.listConversationsForUser(session.getUserId(), session.getRole()),
            "handledBy", Thread.currentThread().getName()
        ));
    }

    private String handleGetMessages(String requestId, Map<String, Object> payload, ChatClientSession session) {
        requireAuth(session);
        Long convId = Long.parseLong(String.valueOf(payload.get("conversationId")));
        List<ChatMessage> msgs = chatService.getConversationMessages(convId, session.getUserId(), session.getRole());
        Map<Integer, String> nameCache = new java.util.HashMap<>();
        List<Map<String, Object>> enriched = new java.util.ArrayList<>();
        for (ChatMessage msg : msgs) {
            Integer sid = msg.getSenderId();
            String senderName = nameCache.computeIfAbsent(sid, id -> {
                String resolved = chatService.resolveUserDisplayName(id);
                return (resolved != null && !resolved.isBlank()) ? resolved : ("User #" + id);
            });
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("id", msg.getId());
            item.put("messageId", msg.getId());
            item.put("conversationId", msg.getConversationId());
            item.put("senderId", sid);
            item.put("senderName", senderName);
            item.put("filteredContent", msg.getFilteredContent());
            item.put("createdAt", msg.getCreatedAt() != null ? msg.getCreatedAt().toString() : "");
            enriched.add(item);
        }
        return okResponse(requestId, "MESSAGES_LIST", Map.of(
            "conversationId", convId,
            "messages", enriched,
            "handledBy", Thread.currentThread().getName()
        ));
    }

    private String handleSendChat(String requestId, Map<String, Object> payload, ChatClientSession session) {
        requireAuth(session);
        Long convId = Long.parseLong(String.valueOf(payload.get("conversationId")));
        String content = str(payload.get("content"));

        SendMessageResult res = chatService.sendMessage(convId, session.getUserId(), session.getRole(), content);
        if (res.blocked()) {
            return errorResponse(requestId, "MESSAGE_BLOCKED",
                "Tin nh\u1eafn b\u1ecb ch\u1eb7n do ch\u1ee9a t\u1eeb c\u1ea5m: " + String.join(", ", res.matchedWords()));
        }

        Map<String, Object> msgPayload = new LinkedHashMap<>();
        msgPayload.put("conversationId", convId);
        msgPayload.put("messageId", res.savedMessage().getId());
        msgPayload.put("senderId", session.getUserId());
        msgPayload.put("senderName", session.getUsername());
        msgPayload.put("senderRole", session.getRole());
        msgPayload.put("content", res.savedMessage().getFilteredContent());
        msgPayload.put("masked", res.masked());
        msgPayload.put("createdAt", res.savedMessage().getCreatedAt().toString());
        msgPayload.put("handledBy", Thread.currentThread().getName());

        boolean isInternal = "INTERNAL".equals(res.conversation().getStatus());
        Set<Integer> memberIds = isInternal ? chatService.getConversationParticipantIds(convId) : Set.of();
        sessionRegistry.pushChatToConversationAudience(res.conversation().getCustomerId(), isInternal, memberIds, msgPayload);
        return okResponse(requestId, "CHAT_SENT", msgPayload);
    }

    private String handleAddBannedWord(String requestId, Map<String, Object> payload, ChatClientSession session) {
        requireRole(session, "ADMIN");
        String word = str(payload.get("word"));
        String actionStr = str(payload.get("action"));
        FilterAction action = "BLOCK".equalsIgnoreCase(actionStr) ? FilterAction.BLOCK : FilterAction.MASK;
        BannedWord saved = bannedWordFilterService.addOrUpdateWord(word, action, "HIGH", session.getUserId());
        sessionRegistry.broadcastEvent("BANNED_WORDS_UPDATED", Map.of("wordId", saved.getId()));
        return okResponse(requestId, "BANNED_WORD_SAVED", Map.of(
            "word", saved,
            "handledBy", Thread.currentThread().getName()
        ));
    }

    private String handleDeleteBannedWord(String requestId, Map<String, Object> payload, ChatClientSession session) {
        requireRole(session, "ADMIN");
        Integer wordId = intVal(payload.get("wordId"));
        bannedWordFilterService.deleteWord(wordId);
        sessionRegistry.broadcastEvent("BANNED_WORDS_UPDATED", Map.of("deletedId", wordId));
        return okResponse(requestId, "BANNED_WORD_DELETED", Map.of(
            "deletedId", wordId,
            "handledBy", Thread.currentThread().getName()
        ));
    }

    private void requireAuth(ChatClientSession session) {
        if (!session.isAuthenticated()) {
            throw new SecurityException("Vui l\u00f2ng \u0111\u0103ng nh\u1eadp (AUTH) tr\u01b0\u1edbc khi th\u1ef1c hi\u1ec7n thao t\u00e1c n\u00e0y");
        }
    }

    private void requireRole(ChatClientSession session, String... allowedRoles) {
        requireAuth(session);
        for (String r : allowedRoles) {
            if (r.equalsIgnoreCase(session.getRole())) return;
        }
        throw new SecurityException("Quy\u1ec1n " + session.getRole() + " kh\u00f4ng \u0111\u01b0\u1ee3c ph\u00e9p th\u1ef1c hi\u1ec7n thao t\u00e1c n\u00e0y");
    }

    private String okResponse(String requestId, String type, Object payload) {
        try {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("v", 2);
            res.put("type", type);
            if (requestId != null) res.put("requestId", requestId);
            res.put("ok", true);
            res.put("timestamp", Instant.now().toString());
            res.put("payload", payload);
            return objectMapper.writeValueAsString(res);
        } catch (Exception e) {
            return "{\"v\":2,\"ok\":false,\"error\":\"SERIALIZATION_ERROR\"}";
        }
    }

    private String errorResponse(String requestId, String code, String message) {
        try {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("v", 2);
            res.put("type", "ERROR");
            if (requestId != null) res.put("requestId", requestId);
            res.put("ok", false);
            res.put("errorCode", code);
            res.put("error", message);
            res.put("timestamp", Instant.now().toString());
            return objectMapper.writeValueAsString(res);
        } catch (Exception e) {
            return "{\"v\":2,\"ok\":false,\"error\":\"SERIALIZATION_ERROR\"}";
        }
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static Integer intVal(Object o) {
        if (o == null) throw new IllegalArgumentException("Thi\u1ebfu tham s\u1ed1 s\u1ed1 nguy\u00ean b\u1eaft bu\u1ed9c");
        return Integer.parseInt(String.valueOf(o));
    }
}