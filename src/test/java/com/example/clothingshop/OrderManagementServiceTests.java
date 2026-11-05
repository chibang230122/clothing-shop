
package com.example.clothingshop;

import com.example.clothingshop.entity.ShopOrder;
import com.example.clothingshop.service.OrderManagementService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderManagementServiceTests {

    private EntityManager entityManager;
    private OrderManagementService service;
    private ShopOrder order;

    @BeforeEach
    void setUp() {
        entityManager = mock(EntityManager.class);

        service = new OrderManagementService();
        ReflectionTestUtils.setField(service, "entityManager", entityManager);

        order = new ShopOrder();
        order.setOrderId(1L);
        order.setOrderStatus("PENDING");

        when(entityManager.find(
                eq(ShopOrder.class),
                eq(1L),
                eq(LockModeType.PESSIMISTIC_WRITE)
        )).thenReturn(order);
    }

    @Test
    void updateStatus_pendingToConfirmed_shouldSuccess() {
        service.updateOrderStatus(1L, "CONFIRMED");

        assertEquals("CONFIRMED", order.getOrderStatus());
    }

    @Test
    void updateStatus_confirmedToProcessing_shouldSuccess() {
        order.setOrderStatus("CONFIRMED");

        service.updateOrderStatus(1L, "PROCESSING");

        assertEquals("PROCESSING", order.getOrderStatus());
    }

    @Test
    void updateStatus_processingToShipping_shouldSuccess() {
        order.setOrderStatus("PROCESSING");

        service.updateOrderStatus(1L, "SHIPPING");

        assertEquals("SHIPPING", order.getOrderStatus());
    }

    @Test
    void updateStatus_shippingToCompleted_shouldSuccess() {
        order.setOrderStatus("SHIPPING");

        service.updateOrderStatus(1L, "COMPLETED");

        assertEquals("COMPLETED", order.getOrderStatus());
    }

    @Test
    void updateStatus_shouldRejectInvalidTransition() {
        assertThrows(IllegalArgumentException.class,
                () -> service.updateOrderStatus(1L, "COMPLETED"));

        assertEquals("PENDING", order.getOrderStatus());
    }

    @Test
    void updateStatus_shouldRejectCancelledStatus() {
        assertThrows(IllegalArgumentException.class,
                () -> service.updateOrderStatus(1L, "CANCELLED"));
    }

    @Test
    void updateStatus_shouldRejectBlankStatus() {
        assertThrows(IllegalArgumentException.class,
                () -> service.updateOrderStatus(1L, " "));
    }

    @Test
    void updateStatus_shouldRejectNullStatus() {
        assertThrows(IllegalArgumentException.class,
                () -> service.updateOrderStatus(1L, null));
    }

    @Test
    void updateStatus_shouldRejectWhenOrderNotFound() {
        when(entityManager.find(
                eq(ShopOrder.class),
                eq(99L),
                eq(LockModeType.PESSIMISTIC_WRITE)
        )).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> service.updateOrderStatus(99L, "CONFIRMED"));
    }

    @Test
    void updateStatus_shouldAcceptLowercaseStatus() {
        service.updateOrderStatus(1L, "confirmed");

        assertEquals("CONFIRMED", order.getOrderStatus());
    }
}
