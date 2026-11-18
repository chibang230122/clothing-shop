
package com.example.clothingshop.service;

import com.example.clothingshop.entity.Cart;
import com.example.clothingshop.entity.CartItem;
import com.example.clothingshop.entity.ProductVariant;
import com.example.clothingshop.entity.User;
import com.example.clothingshop.repository.CartItemRepository;
import com.example.clothingshop.repository.CartRepository;
import com.example.clothingshop.repository.ProductVariantRepository;
import com.example.clothingshop.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ProductVariantRepository productVariantRepository,
            UserRepository userRepository) {

        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productVariantRepository = productVariantRepository;
        this.userRepository = userRepository;
    }
    @Transactional(readOnly = true)
    public List<CartItem> getCartItems(String email) {

        User user = findUser(email);

        return cartRepository.findByUser_UserId(user.getUserId())
                .map(cart -> cartItemRepository.findByCart_CartId(cart.getCartId()))
                .orElse(Collections.emptyList());
    }
    @Transactional
    public CartItem addProductItem(String email, Integer productId, Integer quantity) {
        if (productId == null) {
            throw new IllegalArgumentException("Sản phẩm không hợp lệ.");
        }

        ProductVariant variant = productVariantRepository
                .findByProduct_ProductIdAndIsActiveTrue(productId)
                .stream()
                .filter(v -> Boolean.TRUE.equals(v.getProduct().getIsActive()))
                .filter(v -> v.getStockQuantity() != null && v.getStockQuantity() > 0)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sản phẩm hiện đã hết hàng hoặc chưa có biến thể kinh doanh."));

        return addItem(email, variant.getVariantId(), quantity);
    }

    @Transactional
    public CartItem addItem(String email, Integer variantId, Integer quantity) {

        if (variantId == null) {
            throw new IllegalArgumentException("Vui lòng chọn biến thể sản phẩm.");
        }

        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Số lượng phải lớn hơn 0.");
        }

        User user = findUser(email);

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Không tìm thấy biến thể sản phẩm."));
        if (!Boolean.TRUE.equals(variant.getIsActive())
                || !Boolean.TRUE.equals(variant.getProduct().getIsActive())) {
            throw new IllegalArgumentException("Sản phẩm hiện không kinh doanh.");
        }

        Cart cart = getOrCreateCart(user);
        CartItem cartItem = cartItemRepository
                .findByCart_CartIdAndProductVariant_VariantId(
                        cart.getCartId(), variantId)
                .orElse(null);

        int newQuantity = quantity;

        if (cartItem != null) {
            newQuantity += cartItem.getQuantity();
        } else {
            cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setProductVariant(variant);
        }

        // Kiểm tra tồn kho
        if (variant.getStockQuantity() == null
                || newQuantity > variant.getStockQuantity()) {
            throw new IllegalArgumentException(
                    "Số lượng vượt quá tồn kho. Hiện còn: "
                            + variant.getStockQuantity());
        }

        cartItem.setQuantity(newQuantity);

        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        return cartItemRepository.save(cartItem);
    }
    @Transactional
    public CartItem updateQuantity(
            String email, Integer variantId, Integer quantity) {

        if (variantId == null) {
            throw new IllegalArgumentException("Biến thể không hợp lệ.");
        }

        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Số lượng phải lớn hơn 0.");
        }

        User user = findUser(email);
        Cart cart = findCart(user);

        CartItem cartItem = cartItemRepository
                .findByCart_CartIdAndProductVariant_VariantId(
                        cart.getCartId(), variantId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Sản phẩm không có trong giỏ hàng."));

        ProductVariant variant = cartItem.getProductVariant();

        if (!Boolean.TRUE.equals(variant.getIsActive())
                || !Boolean.TRUE.equals(variant.getProduct().getIsActive())) {
            throw new IllegalArgumentException("Sản phẩm hiện không kinh doanh.");
        }

        if (variant.getStockQuantity() == null
                || quantity > variant.getStockQuantity()) {
            throw new IllegalArgumentException(
                    "Số lượng vượt quá tồn kho. Hiện còn: "
                            + variant.getStockQuantity());
        }

        cartItem.setQuantity(quantity);
        cart.setUpdatedAt(LocalDateTime.now());

        cartRepository.save(cart);

        return cartItemRepository.save(cartItem);
    }
    @Transactional
    public void removeItem(String email, Integer variantId) {

        User user = findUser(email);
        Cart cart = findCart(user);

        CartItem cartItem = cartItemRepository
                .findByCart_CartIdAndProductVariant_VariantId(
                        cart.getCartId(), variantId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Sản phẩm không có trong giỏ hàng."));

        cartItemRepository.delete(cartItem);

        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
    }
    @Transactional
    public void clearCart(String email) {

        User user = findUser(email);

        cartRepository.findByUser_UserId(user.getUserId())
                .ifPresent(cart -> {
                    cartItemRepository.deleteByCart_CartId(cart.getCartId());
                    cart.setUpdatedAt(LocalDateTime.now());
                    cartRepository.save(cart);
                });
    }
    private User findUser(String email) {

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Không xác định được người dùng.");
        }

        return userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() ->
                        new IllegalArgumentException("Không tìm thấy tài khoản."));
    }
    private Cart findCart(User user) {

        return cartRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Giỏ hàng chưa tồn tại."));
    }
    private Cart getOrCreateCart(User user) {

        return cartRepository.findByUser_UserId(user.getUserId())
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(user);
                    cart.setUpdatedAt(LocalDateTime.now());

                    return cartRepository.save(cart);
                });
    }
}
