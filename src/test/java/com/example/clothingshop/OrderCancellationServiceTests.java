
package com.example.clothingshop;

import com.example.clothingshop.entity.OrderItem;
import com.example.clothingshop.entity.ProductVariant;
import com.example.clothingshop.entity.ShopOrder;
import com.example.clothingshop.entity.User;
import com.example.clothingshop.repository.OrderItemRepository;
import com.example.clothingshop.service.OrderCancellationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderCancellationServiceTests {

    private EntityManager entityManager;
    private OrderItemRepository orderItemRepository;
    private OrderCancellationService service;

    private ShopOrder order;
    private User user;
    private ProductVariant variant;
    private OrderItem item;

    @BeforeEach
    void setUp() {
        entityManager = mock(EntityManager.class);
        orderItemRepository = mock(OrderItemRepository.class);

        service = new OrderCancellationService(orderItemRepository);
        ReflectionTestUtils.setField(service, "entityManager", entityManager);

        user = new User();
        user.setEmail("customer@test.com");

        order = new ShopOrder();
        order.setOrderId(1L);
        order.setUser(user);
        order.setOrderStatus("PENDING");

        variant = new ProductVariant();
        variant.setVariantId(10);
        variant.setStockQuantity(5);

        item = new OrderItem();
        item.setProductVariant(variant);
        item.setQuantity(2);
    }

    @Test
    void cancelPendingOrder_shouldCancelAndRestoreStock() {
        when(entityManager.find(
                eq(ShopOrder.class), eq(1L), eq(LockModeType.PESSIMISTIC_WRITE)
        )).thenReturn(order);

        when(orderItemRepository.findByShopOrder_OrderId(1L))
                .thenReturn(List.of(item));

        when(entityManager.find(
                eq(ProductVariant.class), eq(10), eq(LockModeType.PESSIMISTIC_WRITE)
        )).thenReturn(variant);

        service.cancelOrder(1L, "customer@test.com");

        assertEquals("CANCELLED", order.getOrderStatus());
        assertEquals(7, variant.getStockQuantity());
    }

    @Test
    void cancelOrder_shouldRejectWhenOrderNotFound() {
        when(entityManager.find(
                eq(ShopOrder.class), eq(99L), eq(LockModeType.PESSIMISTIC_WRITE)
        )).thenReturn(null);

        assertThrows(ResponseStatusException.class,
                () -> service.cancelOrder(99L, "customer@test.com"));
    }

    @Test
    void cancelOrder_shouldRejectAnotherCustomerOrder() {
        when(entityManager.find(
                eq(ShopOrder.class), eq(1L), eq(LockModeType.PESSIMISTIC_WRITE)
        )).thenReturn(order);

        assertThrows(ResponseStatusException.class,
                () -> service.cancelOrder(1L, "other@test.com"));

        verify(orderItemRepository, never())
                .findByShopOrder_OrderId(anyLong());
    }

    @Test
    void cancelOrder_shouldRejectWhenOrderIsConfirmed() {
        order.setOrderStatus("CONFIRMED");

        when(entityManager.find(
                eq(ShopOrder.class), eq(1L), eq(LockModeType.PESSIMISTIC_WRITE)
        )).thenReturn(order);

        assertThrows(IllegalStateException.class,
                () -> service.cancelOrder(1L, "customer@test.com"));

        verify(orderItemRepository, never())
                .findByShopOrder_OrderId(anyLong());
    }

    @Test
    void cancelOrder_shouldRejectWhenOrderIsCompleted() {
        order.setOrderStatus("COMPLETED");

        when(entityManager.find(
                eq(ShopOrder.class), eq(1L), eq(LockModeType.PESSIMISTIC_WRITE)
        )).thenReturn(order);

        assertThrows(IllegalStateException.class,
                () -> service.cancelOrder(1L, "customer@test.com"));
    }
}
