package com.clothingshop.socket.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RealtimeSessionRegistry {

    private final Set<ChatClientSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper;

    public RealtimeSessionRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void register(ChatClientSession session) {
        sessions.add(session);
    }

    public void unregister(ChatClientSession session) {
        sessions.remove(session);
    }

    public int activeCount() {
        return sessions.size();
    }

    public void broadcastEvent(String eventType, Object payload) {
        String json = serializeEvent(eventType, payload);
        if (json == null) return;
        for (ChatClientSession s : sessions) {
            if (s.isAuthenticated()) {
                s.sendLine(json);
            }
        }
    }

    public void pushChatToConversationAudience(Integer customerId, boolean isInternalRoom, Object payload) {
        pushChatToConversationAudience(customerId, isInternalRoom, Set.of(), payload);
    }

    public void pushChatToConversationAudience(Integer customerId,
                                               boolean isInternalRoom,
                                               Set<Integer> internalMemberIds,
                                               Object payload) {
        String json = serializeEvent("CHAT_MESSAGE", payload);
        if (json == null) return;
        for (ChatClientSession s : sessions) {
            if (!s.isAuthenticated()) continue;
            boolean isStaffOrAdmin = "STAFF".equals(s.getRole()) || "ADMIN".equals(s.getRole());
            if (isInternalRoom) {
                if (isStaffOrAdmin && s.getUserId() != null && internalMemberIds.contains(s.getUserId())) {
                    s.sendLine(json);
                }
            } else {
                boolean isOwnerCustomer = customerId != null && customerId.equals(s.getUserId());
                if (isStaffOrAdmin || isOwnerCustomer) {
                    s.sendLine(json);
                }
            }
        }
    }

    private String serializeEvent(String eventType, Object payload) {
        try {
            Map<String, Object> frame = new LinkedHashMap<>();
            frame.put("v", 2);
            frame.put("type", eventType);
            frame.put("ok", true);
            frame.put("timestamp", Instant.now().toString());
            frame.put("payload", payload);
            return objectMapper.writeValueAsString(frame);
        } catch (Exception e) {
            return null;
        }
    }
}