
package com.example.clothingshop.service;

import com.example.clothingshop.entity.OrderItem;
import com.example.clothingshop.entity.ProductVariant;
import com.example.clothingshop.entity.ShopOrder;
import com.example.clothingshop.repository.OrderItemRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class OrderCancellationService {

    private final OrderItemRepository orderItemRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public OrderCancellationService(
            OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional
    public void cancelOrder(Long orderId, String email) {

        ShopOrder order = entityManager.find(
                ShopOrder.class,
                orderId,
                LockModeType.PESSIMISTIC_WRITE
        );

        if (order == null ||
                !order.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy đơn hàng"
            );
        }

        if (!"PENDING".equals(order.getOrderStatus())) {
            throw new IllegalStateException(
                    "Chỉ được hủy đơn hàng đang chờ xác nhận"
            );
        }

        List<OrderItem> orderItems =
                orderItemRepository.findByShopOrder_OrderId(orderId);

        for (OrderItem item : orderItems) {

            ProductVariant variant = entityManager.find(
                    ProductVariant.class,
                    item.getProductVariant().getVariantId(),
                    LockModeType.PESSIMISTIC_WRITE
            );

            if (variant == null) {
                throw new IllegalStateException(
                        "Không tìm thấy biến thể sản phẩm"
                );
            }

            variant.setStockQuantity(
                    variant.getStockQuantity() + item.getQuantity()
            );
        }

        order.setOrderStatus("CANCELLED");
    }
}
