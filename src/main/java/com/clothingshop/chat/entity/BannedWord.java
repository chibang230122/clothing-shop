package com.clothingshop.chat.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "BannedWords")
public class BannedWord {

    public enum FilterAction {
        MASK, BLOCK
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "WordId")
    private Integer id;

    @Column(name = "Word", nullable = false, length = 100)
    private String word;

    @Column(name = "NormalizedWord", nullable = false, unique = true, length = 100)
    private String normalizedWord;

    @Enumerated(EnumType.STRING)
    @Column(name = "Action", nullable = false, length = 10)
    private FilterAction action = FilterAction.MASK;

    @Column(name = "Severity", nullable = false, length = 15)
    private String severity = "MEDIUM";

    @Column(name = "IsActive", nullable = false)
    private boolean active = true;

    @Column(name = "CreatedBy")
    private Integer createdBy;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }
    public String getNormalizedWord() { return normalizedWord; }
    public void setNormalizedWord(String normalizedWord) { this.normalizedWord = normalizedWord; }
    public FilterAction getAction() { return action; }
    public void setAction(FilterAction action) { this.action = action; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
