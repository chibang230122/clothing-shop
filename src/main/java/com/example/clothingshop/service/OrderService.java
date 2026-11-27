
package com.example.clothingshop.service;

import com.example.clothingshop.dto.CheckoutForm;
import com.example.clothingshop.entity.Cart;
import com.example.clothingshop.entity.CartItem;
import com.example.clothingshop.entity.OrderItem;
import com.example.clothingshop.entity.ProductVariant;
import com.example.clothingshop.entity.ShopOrder;
import com.example.clothingshop.entity.StockTransaction;
import com.example.clothingshop.entity.User;
import com.example.clothingshop.repository.CartItemRepository;
import com.example.clothingshop.repository.CartRepository;
import com.example.clothingshop.repository.OrderItemRepository;
import com.example.clothingshop.repository.ShopOrderRepository;
import com.example.clothingshop.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ShopOrderRepository shopOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public OrderService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ShopOrderRepository shopOrderRepository,
            OrderItemRepository orderItemRepository,
            UserRepository userRepository) {

        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.shopOrderRepository = shopOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ShopOrder checkout(String email, CheckoutForm form) {

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Không xác định được tài khoản người dùng.");
        }

        if (form == null) {
            throw new IllegalArgumentException(
                    "Thông tin đặt hàng không hợp lệ.");
        }

        validateCheckoutForm(form);

        User user = userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy tài khoản khách hàng."));

        if (!Boolean.TRUE.equals(user.getIsActive()) || user.getRole() == null
                || !"CUSTOMER".equals(user.getRole().getRoleName())) {
            throw new AccessDeniedException("Chỉ khách hàng đang hoạt động được đặt hàng.");
        }

        Cart cart = cartRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Bạn chưa có giỏ hàng."));

        List<CartItem> cartItems =
                cartItemRepository.findByCart_CartId(cart.getCartId());

        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException(
                    "Giỏ hàng đang trống, không thể đặt hàng.");
        }

        ShopOrder order = new ShopOrder();

        order.setUser(user);
        order.setOrderCode(generateOrderCode());
        order.setOrderDate(LocalDateTime.now());

        order.setRecipientName(form.getRecipientName().trim());
        order.setRecipientPhone(form.getRecipientPhone().trim());
        order.setShippingAddress(form.getShippingAddress().trim());

        order.setOrderStatus("PENDING");
        order.setPaymentMethod(form.getPaymentMethod());
        order.setPaymentStatus("UNPAID");

        order.setNote(
                form.getNote() == null || form.getNote().trim().isEmpty()
                        ? null
                        : form.getNote().trim()
        );

        BigDecimal subtotal = BigDecimal.ZERO;

        order.setSubtotal(BigDecimal.ZERO);
        order.setShippingFee(BigDecimal.ZERO);
        order.setTotalAmount(BigDecimal.ZERO);

        order = shopOrderRepository.saveAndFlush(order);

        for (CartItem cartItem : cartItems) {

            Integer variantId =
                    cartItem.getProductVariant().getVariantId();

            Integer quantity = cartItem.getQuantity();

            if (quantity == null || quantity <= 0) {
                throw new IllegalArgumentException(
                        "Số lượng sản phẩm trong giỏ không hợp lệ.");
            }

            // Lấy khóa ghi trên dòng ProductVariant.
            ProductVariant variant = entityManager.find(
                    ProductVariant.class,
                    variantId,
                    LockModeType.PESSIMISTIC_WRITE
            );

            if (variant == null) {
                throw new IllegalArgumentException(
                        "Không tìm thấy biến thể sản phẩm.");
            }

            // Làm mới dữ liệu sau khi lấy khóa để tránh dùng tồn kho cũ
            // đã được nạp vào persistence context từ CartItem.
            entityManager.refresh(
                    variant,
                    LockModeType.PESSIMISTIC_WRITE
            );

            if (!Boolean.TRUE.equals(variant.getIsActive())
                    || !Boolean.TRUE.equals(
                    variant.getProduct().getIsActive())) {

                throw new IllegalArgumentException(
                        "Sản phẩm "
                                + variant.getProduct().getProductName()
                                + " hiện không còn kinh doanh.");
            }

            Integer stock = variant.getStockQuantity();

            if (stock == null || stock < quantity) {
                throw new IllegalArgumentException(
                        "Sản phẩm "
                                + variant.getProduct().getProductName()
                                + " không đủ số lượng tồn kho. "
                                + "Còn lại: "
                                + (stock == null ? 0 : stock));
            }

            BigDecimal unitPrice =
                    variant.getProduct().getBasePrice();

            if (unitPrice == null
                    || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Giá sản phẩm không hợp lệ.");
            }

            BigDecimal lineTotal = unitPrice.multiply(
                    BigDecimal.valueOf(quantity));

            subtotal = subtotal.add(lineTotal);

            OrderItem orderItem = new OrderItem();

            orderItem.setShopOrder(order);
            orderItem.setProductVariant(variant);
            orderItem.setQuantity(quantity);
            orderItem.setUnitPrice(unitPrice);
            orderItem.setLineTotal(lineTotal);

            orderItemRepository.save(orderItem);

            // Trừ tồn kho trong cùng transaction với việc tạo đơn hàng.
            variant.setStockQuantity(stock - quantity);
            entityManager.persist(new StockTransaction(variant, "SALE", -quantity, stock, stock - quantity, order.getOrderCode(), "Xuất kho khi đặt hàng", user));
        }

        BigDecimal shippingFee = BigDecimal.ZERO;

        order.setSubtotal(subtotal);
        order.setShippingFee(shippingFee);
        order.setTotalAmount(subtotal.add(shippingFee));

        shopOrderRepository.save(order);

        // Chỉ xóa giỏ hàng sau khi xử lý đặt hàng thành công.
        cartItemRepository.deleteByCart_CartId(cart.getCartId());

        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        return order;
    }

    @Transactional
    public ShopOrder checkoutNow(String email, Integer variantId, Integer quantity, CheckoutForm form) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Không xác định được tài khoản người dùng.");
        }
        if (variantId == null || quantity == null || quantity <= 0 || quantity > 99) {
            throw new IllegalArgumentException("Sản phẩm hoặc số lượng mua không hợp lệ.");
        }
        if (form == null) {
            throw new IllegalArgumentException("Thông tin đặt hàng không hợp lệ.");
        }
        validateCheckoutForm(form);

        User user = userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản khách hàng."));
        if (!Boolean.TRUE.equals(user.getIsActive()) || user.getRole() == null
                || !"CUSTOMER".equals(user.getRole().getRoleName())) {
            throw new AccessDeniedException("Chỉ khách hàng đang hoạt động được đặt hàng.");
        }

        ProductVariant variant = entityManager.find(
                ProductVariant.class, variantId, LockModeType.PESSIMISTIC_WRITE);
        if (variant == null || !Boolean.TRUE.equals(variant.getIsActive())
                || !Boolean.TRUE.equals(variant.getProduct().getIsActive())) {
            throw new IllegalArgumentException("Sản phẩm không còn kinh doanh.");
        }
        entityManager.refresh(variant, LockModeType.PESSIMISTIC_WRITE);
        Integer stock = variant.getStockQuantity();
        if (stock == null || stock < quantity) {
            throw new IllegalArgumentException("Sản phẩm không đủ tồn kho. Còn lại: "
                    + (stock == null ? 0 : stock));
        }

        BigDecimal unitPrice = variant.getProduct().getBasePrice();
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá sản phẩm không hợp lệ.");
        }

        ShopOrder order = new ShopOrder();
        order.setUser(user);
        order.setOrderCode(generateOrderCode());
        order.setOrderDate(LocalDateTime.now());
        order.setRecipientName(form.getRecipientName().trim());
        order.setRecipientPhone(form.getRecipientPhone().trim());
        order.setShippingAddress(form.getShippingAddress().trim());
        order.setOrderStatus("PENDING");
        order.setPaymentMethod(form.getPaymentMethod());
        order.setPaymentStatus("UNPAID");
        order.setNote(form.getNote() == null || form.getNote().isBlank() ? null : form.getNote().trim());

        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        order.setSubtotal(lineTotal);
        order.setShippingFee(BigDecimal.ZERO);
        order.setTotalAmount(lineTotal);
        order = shopOrderRepository.saveAndFlush(order);

        OrderItem item = new OrderItem();
        item.setShopOrder(order);
        item.setProductVariant(variant);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setLineTotal(lineTotal);
        orderItemRepository.save(item);

        variant.setStockQuantity(stock - quantity);
        entityManager.persist(new StockTransaction(variant, "SALE", -quantity, stock, stock - quantity, order.getOrderCode(), "Xuất kho khi đặt hàng", user));
        return order;
    }

    private void validateCheckoutForm(CheckoutForm form) {

        if (form.getRecipientName() == null
                || form.getRecipientName().trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Vui lòng nhập tên người nhận.");
        }

        if (form.getRecipientPhone() == null
                || !form.getRecipientPhone().trim()
                .matches("^[0-9+(). -]{9,20}$")) {
            throw new IllegalArgumentException(
                    "Số điện thoại không hợp lệ.");
        }

        if (form.getShippingAddress() == null
                || form.getShippingAddress().trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Vui lòng nhập địa chỉ giao hàng.");
        }

        if (form.getPaymentMethod() == null
                || !List.of("COD", "BANK_TRANSFER")
                .contains(form.getPaymentMethod())) {
            throw new IllegalArgumentException(
                    "Phương thức thanh toán không hợp lệ.");
        }

        if (form.getRecipientName().trim().length() > 100
                || form.getRecipientPhone().trim().length() > 20
                || form.getShippingAddress().trim().length() > 300
                || (form.getNote() != null
                && form.getNote().length() > 500)) {
            throw new IllegalArgumentException(
                    "Thông tin đặt hàng vượt quá độ dài cho phép.");
        }
    }

    private String generateOrderCode() {
        return "DH" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 20)
                .toUpperCase();
    }
}
