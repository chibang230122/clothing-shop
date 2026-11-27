package com.example.clothingshop.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "StockTransactions")
public class StockTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TransactionId")
    private Long transactionId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "VariantId", nullable = false)
    private ProductVariant productVariant;
    @Column(name = "TransactionType", nullable = false, length = 20)
    private String transactionType;
    @Column(name = "QuantityChange", nullable = false)
    private Integer quantityChange;
    @Column(name = "StockBefore", nullable = false)
    private Integer stockBefore;
    @Column(name = "StockAfter", nullable = false)
    private Integer stockAfter;
    @Column(name = "ReferenceCode", length = 50)
    private String referenceCode;
    @Column(name = "Note", length = 500)
    private String note;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "CreatedBy", nullable = false)
    private User createdBy;
    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;
    public StockTransaction() {}
    public Long getTransactionId() { return transactionId; }
    public ProductVariant getProductVariant() { return productVariant; }
    public String getTransactionType() { return transactionType; }
    public Integer getQuantityChange() { return quantityChange; }
    public Integer getStockBefore() { return stockBefore; }
    public Integer getStockAfter() { return stockAfter; }
    public String getReferenceCode() { return referenceCode; }
    public String getNote() { return note; }
    public User getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public StockTransaction(ProductVariant variant, String type, int change, int before, int after, String reference, String note, User user) {
        this.productVariant=variant; this.transactionType=type; this.quantityChange=change; this.stockBefore=before; this.stockAfter=after; this.referenceCode=reference; this.note=note; this.createdBy=user; this.createdAt=LocalDateTime.now();
    }
}
