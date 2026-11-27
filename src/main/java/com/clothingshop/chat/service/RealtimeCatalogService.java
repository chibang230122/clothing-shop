package com.clothingshop.chat.service;

import com.clothingshop.chat.entity.StockAdjustmentLog;
import com.example.clothingshop.entity.Product;
import com.example.clothingshop.entity.ProductVariant;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RealtimeCatalogService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listCatalogSnapshot(String keyword) {
        String kw = (keyword == null || keyword.isBlank()) ? null : "%" + keyword.trim().toLowerCase() + "%";
        List<ProductVariant> variants = entityManager.createQuery(
            "SELECT v FROM ProductVariant v JOIN FETCH v.product p " +
            "WHERE (:kw IS NULL OR LOWER(p.productName) LIKE :kw OR LOWER(v.sku) LIKE :kw) " +
            "ORDER BY p.productId ASC, v.sku ASC",
            ProductVariant.class
        ).setParameter("kw", kw).getResultList();

        return variants.stream().map(v -> {
            Product p = v.getProduct();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("productId", p.getProductId());
            row.put("productName", p.getProductName());
            row.put("basePrice", p.getBasePrice());
            row.put("categoryName", p.getCategory() != null ? p.getCategory().getCategoryName() : "Chung");
            row.put("description", p.getDescription() != null ? p.getDescription() : "");
            row.put("active", Boolean.TRUE.equals(p.getIsActive()));
            row.put("productVersion", p.getVersion());
            row.put("variantId", v.getVariantId());
            row.put("sku", v.getSku());
            row.put("size", v.getSize());
            row.put("color", v.getColor());
            row.put("stockQuantity", v.getStockQuantity());
            row.put("variantVersion", v.getVersion());
            return row;
        }).toList();
    }

    @Transactional
    public Map<String, Object> updateProductOptimistic(Integer productId,
                                                       String newName,
                                                       BigDecimal newPrice,
                                                       String newDescription,
                                                       Boolean newActive,
                                                       Long expectedVersion) {
        Product product = entityManager.find(Product.class, productId);
        if (product == null) {
            throw new IllegalArgumentException("Không tìm thấy sản phẩm ID=" + productId);
        }
        if (expectedVersion != null && !expectedVersion.equals(product.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(Product.class, productId);
        }
        if (newName != null && !newName.isBlank()) {
            product.setProductName(newName.trim());
        }
        if (newPrice != null) {
            if (newPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Giá bán không được âm");
            }
            product.setBasePrice(newPrice);
        }
        if (newDescription != null) {
            product.setDescription(newDescription.trim());
        }
        if (newActive != null) {
            product.setIsActive(newActive);
        }
        entityManager.flush();

        Map<String, Object> updated = new LinkedHashMap<>();
        updated.put("productId", product.getProductId());
        updated.put("productName", product.getProductName());
        updated.put("basePrice", product.getBasePrice());
        updated.put("description", product.getDescription());
        updated.put("active", Boolean.TRUE.equals(product.getIsActive()));
        updated.put("productVersion", product.getVersion());
        return updated;
    }

    @Transactional
    public Map<String, Object> updateStockPessimistic(String sku, Integer delta, Integer setExact,
                                                      String reason, String actorUsername) {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU không được rỗng");
        }
        ProductVariant variant;
        try {
            variant = entityManager.createQuery(
                    "SELECT v FROM ProductVariant v WHERE v.sku = :sku", ProductVariant.class)
                .setParameter("sku", sku.trim())
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getSingleResult();
        } catch (NoResultException ex) {
            throw new IllegalArgumentException("Không tìm thấy SKU: " + sku);
        }

        int current = variant.getStockQuantity();
        int next = (setExact != null) ? setExact : (delta != null ? current + delta : -1);
        if (next < 0) {
            throw new IllegalArgumentException("Tồn kho không hợp lệ hoặc bị âm (hiện tại: " + current + ")");
        }

        int actualDelta = next - current;
        LocalDateTime now = LocalDateTime.now();
        String cleanReason = reason != null ? reason.trim() : "";

        variant.setStockQuantity(next);

        StockAdjustmentLog log = new StockAdjustmentLog();
        log.setVariantId(variant.getVariantId());
        log.setSku(variant.getSku());
        log.setPreviousStock(current);
        log.setDeltaStock(actualDelta);
        log.setNewStock(next);
        log.setReason(cleanReason);
        log.setAdjustedBy(actorUsername);
        log.setAdjustedAt(now);
        entityManager.persist(log);
        entityManager.flush();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("logId", log.getId());
        result.put("variantId", variant.getVariantId());
        result.put("productId", variant.getProduct().getProductId());
        result.put("sku", variant.getSku());
        result.put("previousStock", current);
        result.put("deltaStock", actualDelta);
        result.put("stockQuantity", next);
        result.put("reason", cleanReason);
        result.put("adjustedBy", actorUsername);
        result.put("adjustedAt", now.toString());
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listStockHistory(String skuFilter) {
        String filter = (skuFilter == null || skuFilter.isBlank()) ? null : skuFilter.trim().toLowerCase();
        List<StockAdjustmentLog> logs = entityManager.createQuery(
                "SELECT l FROM StockAdjustmentLog l " +
                "WHERE (:sku IS NULL OR LOWER(l.sku) LIKE :sku) " +
                "ORDER BY l.adjustedAt DESC, l.id DESC",
                StockAdjustmentLog.class)
            .setParameter("sku", filter != null ? "%" + filter + "%" : null)
            .setMaxResults(100)
            .getResultList();

        return logs.stream().map(l -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("logId", l.getId());
            row.put("sku", l.getSku());
            row.put("previousStock", l.getPreviousStock());
            row.put("deltaStock", l.getDeltaStock());
            row.put("newStock", l.getNewStock());
            row.put("reason", l.getReason() != null ? l.getReason() : "");
            row.put("adjustedBy", l.getAdjustedBy() != null ? l.getAdjustedBy() : "");
            row.put("adjustedAt", l.getAdjustedAt() != null ? l.getAdjustedAt().toString() : "");
            return row;
        }).toList();
    }
}