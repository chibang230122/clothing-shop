
package com.example.clothingshop.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ImportReceipts")
public class ImportReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ImportReceiptId")
    private Integer importReceiptId;

    @Column(name = "ImportDate", nullable = false)
    private LocalDateTime importDate;

    @Column(name = "TotalAmount", nullable = false,
            precision = 18, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "Note", length = 500)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreatedBy", nullable = false)
    private User createdBy;

    @OneToMany(
            mappedBy = "importReceipt",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ImportReceiptDetail> details = new ArrayList<>();

    public ImportReceipt() {
    }

    public Integer getImportReceiptId() {
        return importReceiptId;
    }

    public void setImportReceiptId(Integer importReceiptId) {
        this.importReceiptId = importReceiptId;
    }

    public LocalDateTime getImportDate() {
        return importDate;
    }

    public void setImportDate(LocalDateTime importDate) {
        this.importDate = importDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public List<ImportReceiptDetail> getDetails() {
        return details;
    }

    public void setDetails(List<ImportReceiptDetail> details) {
        this.details = details;
    }

    public void addDetail(ImportReceiptDetail detail) {
        details.add(detail);
        detail.setImportReceipt(this);
    }
}
