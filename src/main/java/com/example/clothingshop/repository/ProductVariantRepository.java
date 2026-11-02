package com.example.clothingshop.repository;

import com.example.clothingshop.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {

    // Lấy các biến thể đang kinh doanh
    List<ProductVariant> findByProduct_ProductIdAndIsActiveTrue(Integer productId);

    // Lấy tất cả biến thể của sản phẩm
    List<ProductVariant> findByProduct_ProductId(Integer productId);

    // Tìm biến thể theo SKU
    Optional<ProductVariant> findBySku(String sku);

    // Tìm SKU không phân biệt chữ hoa/thường
    Optional<ProductVariant> findBySkuIgnoreCase(String sku);

    // Tìm biến thể theo sản phẩm, size và màu sắc
    Optional<ProductVariant> findByProduct_ProductIdAndSizeIgnoreCaseAndColorIgnoreCase(
            Integer productId, String size, String color);

    // Kiểm tra SKU đã tồn tại, không phân biệt chữ hoa/thường
    boolean existsBySkuIgnoreCase(String sku);

    // Kiểm tra size và màu đã tồn tại trong sản phẩm
    boolean existsByProduct_ProductIdAndSizeIgnoreCaseAndColorIgnoreCase(Integer productId, String size, String color);

    // Lấy danh sách size đang được sử dụng
    @Query("SELECT DISTINCT v.size FROM ProductVariant v WHERE v.isActive = true ORDER BY v.size")
    List<String> findAllActiveSizes();

    // Lấy danh sách màu sắc đang được sử dụng
    @Query("SELECT DISTINCT v.color FROM ProductVariant v WHERE v.isActive = true ORDER BY v.color")
    List<String> findAllActiveColors();

    // Tìm sản phẩm theo SKU, size và màu sắc
    @Query("""
        SELECT DISTINCT v.product.productId
        FROM ProductVariant v
        WHERE v.isActive = true
          AND v.product.isActive = true
          AND (:sku IS NULL OR LOWER(v.sku) LIKE LOWER(CONCAT('%', :sku, '%')))
          AND (:size IS NULL OR LOWER(v.size) = LOWER(:size))
          AND (:color IS NULL OR LOWER(v.color) = LOWER(:color))
        """)
    List<Integer> findProductIdsByVariantFilters(
            @Param("sku") String sku,
            @Param("size") String size,
            @Param("color") String color
    );
}