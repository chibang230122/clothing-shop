package com.clothingshop.chat.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ChatMessages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MessageId")
    private Long id;

    @Column(name = "ConversationId", nullable = false)
    private Long conversationId;

    @Column(name = "SenderId", nullable = false)
    private Integer senderId;

    @Column(name = "OriginalContent", nullable = false, length = 2000)
    private String originalContent;

    @Column(name = "FilteredContent", nullable = false, length = 2000)
    private String filteredContent;

    @Column(name = "IsMasked", nullable = false)
    private boolean masked;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public Integer getSenderId() { return senderId; }
    public void setSenderId(Integer senderId) { this.senderId = senderId; }
    public String getOriginalContent() { return originalContent; }
    public void setOriginalContent(String originalContent) { this.originalContent = originalContent; }
    public String getFilteredContent() { return filteredContent; }
    public void setFilteredContent(String filteredContent) { this.filteredContent = filteredContent; }
    public boolean isMasked() { return masked; }
    public void setMasked(boolean masked) { this.masked = masked; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
