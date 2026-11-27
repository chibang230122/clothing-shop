package com.clothingshop.chat.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ChatParticipants", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"ConversationId", "UserId"})
})
public class ChatParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ParticipantId")
    private Long id;

    @Column(name = "ConversationId", nullable = false)
    private Long conversationId;

    @Column(name = "UserId", nullable = false)
    private Integer userId;

    @Column(name = "RoleInChat", nullable = false, length = 20)
    private String roleInChat;

    @Column(name = "JoinedAt", nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

    @Column(name = "LastReadMessageId")
    private Long lastReadMessageId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getRoleInChat() { return roleInChat; }
    public void setRoleInChat(String roleInChat) { this.roleInChat = roleInChat; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
    public Long getLastReadMessageId() { return lastReadMessageId; }
    public void setLastReadMessageId(Long lastReadMessageId) { this.lastReadMessageId = lastReadMessageId; }
}
