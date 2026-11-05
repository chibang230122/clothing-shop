
package com.example.clothingshop;

import com.example.clothingshop.entity.OrderItem;
import com.example.clothingshop.entity.ProductVariant;
import com.example.clothingshop.entity.ShopOrder;
import com.example.clothingshop.entity.User;

import com.example.clothingshop.repository.OrderItemRepository;

import com.example.clothingshop.service.OrderCancellationService;
import com.example.clothingshop.service.OrderManagementService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderWorkflowServiceTests {

    @Mock
    private EntityManager entityManager;

    @Mock
    private OrderItemRepository orderItemRepository;

    private OrderCancellationService cancellationService;
    private OrderManagementService managementService;

    private User user;
    private ShopOrder order;
    private ProductVariant variant;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {

        cancellationService = new OrderCancellationService(
                orderItemRepository
        );

        managementService = new OrderManagementService();

        ReflectionTestUtils.setField(
                cancellationService,
                "entityManager",
                entityManager
        );

        ReflectionTestUtils.setField(
                managementService,
                "entityManager",
                entityManager
        );

        user = new User();
        user.setUserId(1);
        user.setEmail("customer@gmail.com");

        order = new ShopOrder();
        order.setOrderId(100L);
        order.setUser(user);
        order.setOrderStatus("PENDING");

        variant = new ProductVariant();
        variant.setVariantId(10);
        variant.setStockQuantity(5);

        orderItem = new OrderItem();
        orderItem.setShopOrder(order);
        orderItem.setProductVariant(variant);
        orderItem.setQuantity(2);
    }

    // TEST 1: Huy don thanh cong va hoan kho

    @Test
    void cancelOrder_successAndRestoreStock() {

        when(entityManager.find(
                eq(ShopOrder.class),
                eq(100L),
                eq(LockModeType.PESSIMISTIC_WRITE)
        )).thenReturn(order);

        when(orderItemRepository.findByShopOrder_OrderId(100L))
                .thenReturn(List.of(orderItem));

        when(entityManager.find(
                eq(ProductVariant.class),
                eq(10),
                eq(LockModeType.PESSIMISTIC_WRITE)
        )).thenReturn(variant);

        cancellationService.cancelOrder(
                100L,
                "customer@gmail.com"
        );

        assertEquals("CANCELLED", order.getOrderStatus());

        assertEquals(7, variant.getStockQuantity());

        System.out.println(
                "TEST 1 PASSED: Huy don va hoan kho thanh cong"
        );
    }

    // TEST 2: Khong cho khach huy don cua nguoi khac

    @Test
    void cancelOrder_wrongCustomer() {

        when(entityManager.find(
                eq(ShopOrder.class),
                eq(100L),
                eq(LockModeType.PESSIMISTIC_WRITE)
        )).thenReturn(order);

        assertThrows(
                ResponseStatusException.class,
                () -> cancellationService.cancelOrder(
                        100L,
                        "other@gmail.com"
                )
        );

        verify(orderItemRepository, never())
                .findByShopOrder_OrderId(anyLong());

        assertEquals("PENDING", order.getOrderStatus());

        System.out.println(
                "TEST 2 PASSED: Chan huy don cua nguoi khac"
        );
    }

    // TEST 3: Khong cho huy don da xac nhan

    @Test
    void cancelOrder_confirmedOrder() {

        order.setOrderStatus("CONFIRMED");

        when(entityManager.find(
                eq(ShopOrder.class),
                eq(100L),
                eq(LockModeType.PESSIMISTIC_WRITE)
        )).thenReturn(order);

        assertThrows(
                IllegalStateException.class,
                () -> cancellationService.cancelOrder(
                        100L,
                        "customer@gmail.com"
                )
        );

        verify(orderItemRepository, never())
                .findByShopOrder_OrderId(anyLong());

        assertEquals("CONFIRMED", order.getOrderStatus());

        System.out.println(
                "TEST 3 PASSED: Chan huy don da xac nhan"
        );
    }

    // TEST 4: Don hang khong ton tai

    @Test
    void cancelOrder_notFound() {

        when(entityManager.find(
                eq(ShopOrder.class),
                eq(999L),
                eq(LockModeType.PESSIMISTIC_WRITE)
        )).thenReturn(null);

        assertThrows(
                ResponseStatusException.class,
                () -> cancellationService.cancelOrder(
                        999L,
                        "customer@gmail.com"
                )
        );

        verify(orderItemRepository, never())
                .findByShopOrder_OrderId(anyLong());

        System.out.println(
                "TEST 4 PASSED: Xu ly don hang khong ton tai"
        );
    }

    // TEST 5: Chuyen trang thai PENDING sang CONFIRMED

    @Test
    void updateStatus_pendingToConfirmed() {

        when(entityManager.find(
                eq(ShopOrder.class),
                eq(100L),
                eq(LockModeType.PESSIMISTIC_WRITE)
        )).thenReturn(order);

        managementService.updateOrderStatus(
                100L,
                "CONFIRMED"
        );

        assertEquals("CONFIRMED", order.getOrderStatus());

        System.out.println(
                "TEST 5 PASSED: Xac nhan don hang"
        );
    }

    // TEST 6: Chan chuyen trang thai khong hop le

    @Test
    void updateStatus_invalidTransition() {

        when(entityManager.find(
                eq(ShopOrder.class),
                eq(100L),
                eq(LockModeType.PESSIMISTIC_WRITE)
        )).thenReturn(order);

        assertThrows(
                IllegalArgumentException.class,
                () -> managementService.updateOrderStatus(
                        100L,
                        "COMPLETED"
                )
        );

        assertEquals("PENDING", order.getOrderStatus());

        System.out.println(
                "TEST 6 PASSED: Chan chuyen trang thai sai"
        );
    }

    // TEST 7: Khong cho cap nhat trang thai rong

    @Test
    void updateStatus_blankStatus() {

        when(entityManager.find(
                eq(ShopOrder.class),
                eq(100L),
                eq(LockModeType.PESSIMISTIC_WRITE)
        )).thenReturn(order);

        assertThrows(
                IllegalArgumentException.class,
                () -> managementService.updateOrderStatus(
                        100L,
                        " "
                )
        );

        assertEquals("PENDING", order.getOrderStatus());

        System.out.println(
                "TEST 7 PASSED: Chan trang thai rong"
        );
    }

    // TEST 8: Khong tim thay don khi cap nhat trang thai

    @Test
    void updateStatus_orderNotFound() {

        when(entityManager.find(
                eq(ShopOrder.class),
                eq(999L),
                eq(LockModeType.PESSIMISTIC_WRITE)
        )).thenReturn(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> managementService.updateOrderStatus(
                        999L,
                        "CONFIRMED"
                )
        );

        System.out.println(
                "TEST 8 PASSED: Don hang khong ton tai"
        );
    }

    // TEST 9: Hoan thanh luong trang thai hop le

    @Test
    void updateStatus_completeWorkflow() {

        when(entityManager.find(
                eq(ShopOrder.class),
                eq(100L),
                eq(LockModeType.PESSIMISTIC_WRITE)
        )).thenReturn(order);

        managementService.updateOrderStatus(100L, "CONFIRMED");
        assertEquals("CONFIRMED", order.getOrderStatus());

        managementService.updateOrderStatus(100L, "PROCESSING");
        assertEquals("PROCESSING", order.getOrderStatus());

        managementService.updateOrderStatus(100L, "SHIPPING");
        assertEquals("SHIPPING", order.getOrderStatus());

        managementService.updateOrderStatus(100L, "COMPLETED");
        assertEquals("COMPLETED", order.getOrderStatus());

        System.out.println(
                "TEST 9 PASSED: Hoan thanh luong trang thai don hang"
        );
    }
}
