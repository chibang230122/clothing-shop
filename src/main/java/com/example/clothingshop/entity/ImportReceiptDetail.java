
package com.example.clothingshop.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ImportReceiptDetails")
public class ImportReceiptDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ImportDetailId")
    private Integer importDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ImportReceiptId", nullable = false)
    private ImportReceipt importReceipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VariantId", nullable = false)
    private ProductVariant productVariant;

    @Column(name = "Quantity", nullable = false)
    private Integer quantity;

    @Column(name = "ImportPrice", nullable = false,
            precision = 18, scale = 2)
    private BigDecimal importPrice;

    @Column(name = "Subtotal", nullable = false,
            precision = 18, scale = 2)
    private BigDecimal subtotal;

    public ImportReceiptDetail() {
    }

    public Integer getImportDetailId() {
        return importDetailId;
    }

    public void setImportDetailId(Integer importDetailId) {
        this.importDetailId = importDetailId;
    }

    public ImportReceipt getImportReceipt() {
        return importReceipt;
    }

    public void setImportReceipt(ImportReceipt importReceipt) {
        this.importReceipt = importReceipt;
    }

    public ProductVariant getProductVariant() {
        return productVariant;
    }

    public void setProductVariant(ProductVariant productVariant) {
        this.productVariant = productVariant;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getImportPrice() {
        return importPrice;
    }

    public void setImportPrice(BigDecimal importPrice) {
        this.importPrice = importPrice;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}
