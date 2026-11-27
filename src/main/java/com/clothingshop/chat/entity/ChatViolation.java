package com.clothingshop.chat.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ChatViolations")
public class ChatViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ViolationId")
    private Long id;

    @Column(name = "UserId", nullable = false)
    private Integer userId;

    @Column(name = "ConversationId")
    private Long conversationId;

    @Column(name = "RawContent", nullable = false, length = 2000)
    private String rawContent;

    @Column(name = "MatchedWord", nullable = false, length = 100)
    private String matchedWord;

    @Column(name = "ActionTaken", nullable = false, length = 10)
    private String actionTaken;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public String getRawContent() { return rawContent; }
    public void setRawContent(String rawContent) { this.rawContent = rawContent; }
    public String getMatchedWord() { return matchedWord; }
    public void setMatchedWord(String matchedWord) { this.matchedWord = matchedWord; }
    public String getActionTaken() { return actionTaken; }
    public void setActionTaken(String actionTaken) { this.actionTaken = actionTaken; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
