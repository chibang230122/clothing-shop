package com.example.clothingshop.concurrency;

import com.example.clothingshop.entity.ProductVariant;
import com.example.clothingshop.network.StockInfo;
import com.example.clothingshop.network.StockLookup;
import com.example.clothingshop.repository.ProductVariantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Tra cứu tồn kho cho Socket Server.
 *
 * Phục vụ nhiều client cùng lúc nên chỉ ĐỌC dữ liệu đã commit trong transaction chỉ-đọc, mỗi luồng
 * dùng một phiên làm việc riêng. Việc GHI tồn kho (trừ khi đặt hàng, hoàn khi hủy) chỉ nằm ở
 * OrderService và OrderCancellationService, có khóa PESSIMISTIC_WRITE trên dòng ProductVariants, nên
 * giá trị client đọc được không bao giờ âm và không phản ánh transaction đang dở dang.
 */
@Service
public class InventoryService implements StockLookup {

    private final ProductVariantRepository variantRepository;

    public InventoryService(ProductVariantRepository variantRepository) {
        this.variantRepository = variantRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StockInfo> findStockBySku(String sku) {
        return variantRepository.findBySkuIgnoreCase(sku).map(this::toStockInfo);
    }

    private StockInfo toStockInfo(ProductVariant variant) {
        boolean variantActive = Boolean.TRUE.equals(variant.getIsActive());
        boolean productActive = variant.getProduct() != null
                && Boolean.TRUE.equals(variant.getProduct().getIsActive());
        String productName = variant.getProduct() == null ? "" : variant.getProduct().getProductName();
        int stock = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();

        return new StockInfo(
                variant.getSku(),
                productName,
                variant.getSize(),
                variant.getColor(),
                stock,
                variantActive && productActive);
    }
}
