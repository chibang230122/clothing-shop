package com.clothingshop.chat.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "StockAdjustmentLogs")
public class StockAdjustmentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LogId")
    private Integer id;

    @Column(name = "VariantId", nullable = false)
    private Integer variantId;

    @Column(name = "Sku", nullable = false, length = 100)
    private String sku;

    @Column(name = "PreviousStock", nullable = false)
    private Integer previousStock;

    @Column(name = "DeltaStock", nullable = false)
    private Integer deltaStock;

    @Column(name = "NewStock", nullable = false)
    private Integer newStock;

    @Column(name = "Reason", length = 500)
    private String reason;

    @Column(name = "AdjustedBy", length = 150)
    private String adjustedBy;

    @Column(name = "AdjustedAt", nullable = false)
    private LocalDateTime adjustedAt = LocalDateTime.now();

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getVariantId() { return variantId; }
    public void setVariantId(Integer variantId) { this.variantId = variantId; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public Integer getPreviousStock() { return previousStock; }
    public void setPreviousStock(Integer previousStock) { this.previousStock = previousStock; }
    public Integer getDeltaStock() { return deltaStock; }
    public void setDeltaStock(Integer deltaStock) { this.deltaStock = deltaStock; }
    public Integer getNewStock() { return newStock; }
    public void setNewStock(Integer newStock) { this.newStock = newStock; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getAdjustedBy() { return adjustedBy; }
    public void setAdjustedBy(String adjustedBy) { this.adjustedBy = adjustedBy; }
    public LocalDateTime getAdjustedAt() { return adjustedAt; }
    public void setAdjustedAt(LocalDateTime adjustedAt) { this.adjustedAt = adjustedAt; }
}