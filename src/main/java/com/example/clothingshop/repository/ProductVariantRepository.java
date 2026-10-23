
package com.example.clothingshop.repository;

import com.example.clothingshop.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {

    List<ProductVariant> findByProduct_ProductIdAndIsActiveTrue(Integer productId);

    Optional<ProductVariant> findBySku(String sku);
}
