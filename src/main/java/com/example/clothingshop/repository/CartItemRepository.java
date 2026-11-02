
package com.example.clothingshop.repository;

import com.example.clothingshop.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCart_CartId(Long cartId);

    Optional<CartItem> findByCart_CartIdAndProductVariant_VariantId(
            Long cartId, Integer variantId);

    void deleteByCart_CartId(Long cartId);
}
