package com.example.clothingshop.repository;

import com.example.clothingshop.entity.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Integer> {

    List<ProductReview> findByProduct_ProductIdOrderByCreatedAtDesc(Integer productId);

    boolean existsByProduct_ProductIdAndUser_EmailIgnoreCase(Integer productId, String email);

    @Query(value = """
        SELECT COUNT(*) FROM OrderItems oi
        JOIN Orders o ON o.OrderId = oi.OrderId
        JOIN Users u ON u.UserId = o.UserId
        JOIN ProductVariants pv ON pv.VariantId = oi.VariantId
        WHERE LOWER(u.Email) = LOWER(:email)
          AND o.OrderStatus = 'COMPLETED'
          AND pv.ProductId = :productId
    """, nativeQuery = true)
    int countCompletedPurchases(@Param("email") String email, @Param("productId") Integer productId);
}