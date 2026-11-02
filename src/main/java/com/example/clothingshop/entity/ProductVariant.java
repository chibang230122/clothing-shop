
package com.example.clothingshop.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "ProductVariants",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UQ_ProductVariants_Product_Size_Color",
                        columnNames = {"ProductId", "Size", "Color"}
                )
        }
)
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "VariantId")
    private Integer variantId;

    @ManyToOne
    @JoinColumn(name = "ProductId", nullable = false)
    private Product product;

    @Column(name = "SKU", nullable = false, unique = true, length = 50)
    private String sku;

    @Column(name = "Size", nullable = false, length = 10)
    private String size;

    @Column(name = "Color", nullable = false, length = 40)
    private String color;

    @Column(name = "StockQuantity", nullable = false)
    private Integer stockQuantity = 0;

    @Column(name = "IsActive", nullable = false)
    private Boolean isActive = true;

    public ProductVariant() {
    }

    public Integer getVariantId() {
        return variantId;
    }

    public void setVariantId(Integer variantId) {
        this.variantId = variantId;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
