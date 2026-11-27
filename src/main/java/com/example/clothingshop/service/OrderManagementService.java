
package com.example.clothingshop.service;

import com.example.clothingshop.entity.ShopOrder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class OrderManagementService {

    @PersistenceContext
    private EntityManager entityManager;

    private final Map<String, Set<String>> allowedTransitions = Map.of(
            "PENDING", Set.of("CONFIRMED"),
            "CONFIRMED", Set.of("PROCESSING"),
            "PROCESSING", Set.of("SHIPPING"),
            "SHIPPING", Set.of("COMPLETED")
    );

    @Transactional
    public void updateOrderStatus(Long orderId, String newStatus) {

        ShopOrder order = entityManager.find(
                ShopOrder.class,
                orderId,
                LockModeType.PESSIMISTIC_WRITE
        );

        if (order == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng");
        }

        if (newStatus == null || newStatus.isBlank()) {
            throw new IllegalArgumentException("Trạng thái đơn hàng không hợp lệ");
        }

        String currentStatus = order.getOrderStatus();

        newStatus = newStatus.trim().toUpperCase(Locale.ROOT);

        if (currentStatus == null || !allowedTransitions.containsKey(currentStatus)) {
            throw new IllegalArgumentException("Trạng thái đơn hàng không hợp lệ");
        }

        if (!allowedTransitions.get(currentStatus).contains(newStatus)) {
            throw new IllegalArgumentException(
                    "Không thể chuyển từ " + currentStatus + " sang " + newStatus
            );
        }

        order.setOrderStatus(newStatus);
        if ("COMPLETED".equals(newStatus) && "COD".equals(order.getPaymentMethod())) {
            order.setPaymentStatus("PAID");
        }
    }
}
