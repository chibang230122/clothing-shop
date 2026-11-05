
package com.example.clothingshop;

import com.example.clothingshop.dto.CheckoutForm;
import com.example.clothingshop.entity.*;
import com.example.clothingshop.repository.*;
import com.example.clothingshop.service.OrderService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTests {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ShopOrderRepository shopOrderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EntityManager entityManager;

    private OrderService orderService;

    private User user;
    private Cart cart;
    private Product product;
    private ProductVariant variant;
    private CartItem cartItem;
    private CheckoutForm form;

    @BeforeEach
    void setUp() {

        orderService = new OrderService(
                cartRepository,
                cartItemRepository,
                shopOrderRepository,
                orderItemRepository,
                userRepository
        );

        // Inject EntityManager vao truong @PersistenceContext
        ReflectionTestUtils.setField(
                orderService,
                "entityManager",
                entityManager
        );

        user = new User();
        user.setUserId(1);
        user.setEmail("customer@gmail.com");
        user.setFullName("Nguyen Van A");
        user.setIsActive(true);

        cart = new Cart();
        cart.setCartId(1L);
        cart.setUser(user);
        cart.setUpdatedAt(LocalDateTime.now());

        product = new Product();
        product.setProductId(1);
        product.setProductName("Ao thun");
        product.setBasePrice(new BigDecimal("150000"));
        product.setIsActive(true);

        variant = new ProductVariant();
        variant.setVariantId(10);
        variant.setProduct(product);
        variant.setSku("AT001");
        variant.setSize("L");
        variant.setColor("Den");
        variant.setStockQuantity(10);
        variant.setIsActive(true);

        cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setProductVariant(variant);
        cartItem.setQuantity(2);

        form = new CheckoutForm();
        form.setRecipientName("Nguyen Van A");
        form.setRecipientPhone("0901234567");
        form.setShippingAddress("TP Ho Chi Minh");
        form.setPaymentMethod("COD");
        form.setNote("Giao gio hanh chinh");
    }

    // Khoi tao thong tin tai khoan va gio hang

    private void mockUserAndCart(List<CartItem> items) {

        when(userRepository.findByEmailIgnoreCase(
                "customer@gmail.com"))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUser_UserId(1))
                .thenReturn(Optional.of(cart));

        when(cartItemRepository.findByCart_CartId(1L))
                .thenReturn(items);
    }

    // Mock luu don hang

    private void mockSaveOrder() {

        when(shopOrderRepository.saveAndFlush(any(ShopOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // TEST 1: Dat hang thanh cong

    @Test
    void checkout_success() {

        mockUserAndCart(List.of(cartItem));
        mockSaveOrder();

        when(entityManager.find(
                eq(ProductVariant.class),
                eq(10),
                eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(variant);

        when(shopOrderRepository.save(any(ShopOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShopOrder result = orderService.checkout(
                "customer@gmail.com",
                form
        );

        assertNotNull(result);

        assertEquals("PENDING", result.getOrderStatus());
        assertEquals("COD", result.getPaymentMethod());

        assertEquals(
                new BigDecimal("300000"),
                result.getSubtotal()
        );

        assertEquals(
                new BigDecimal("300000"),
                result.getTotalAmount()
        );

        assertEquals(8, variant.getStockQuantity());

        verify(orderItemRepository, times(1))
                .save(any(OrderItem.class));

        verify(cartItemRepository, times(1))
                .deleteByCart_CartId(1L);

        verify(cartRepository, times(1))
                .save(cart);

        System.out.println("TEST 1 PASSED: Dat hang thanh cong");
    }

    // TEST 2: Ton kho khong du

    @Test
    void checkout_insufficientStock() {

        variant.setStockQuantity(1);

        mockUserAndCart(List.of(cartItem));
        mockSaveOrder();

        when(entityManager.find(
                eq(ProductVariant.class),
                eq(10),
                eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(variant);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> orderService.checkout(
                                "customer@gmail.com",
                                form
                        )
                );

        assertTrue(
                exception.getMessage().contains("không đủ số lượng")
        );

        assertEquals(1, variant.getStockQuantity());

        verify(orderItemRepository, never())
                .save(any(OrderItem.class));

        verify(cartItemRepository, never())
                .deleteByCart_CartId(anyLong());

        System.out.println("TEST 2 PASSED: Chan dat hang khi het hang");
    }

    // TEST 3: Gio hang trong

    @Test
    void checkout_emptyCart() {

        mockUserAndCart(List.of());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> orderService.checkout(
                                "customer@gmail.com",
                                form
                        )
                );

        assertTrue(
                exception.getMessage().contains("đang trống")
        );

        verify(shopOrderRepository, never())
                .saveAndFlush(any(ShopOrder.class));

        System.out.println("TEST 3 PASSED: Chan gio hang rong");
    }

    // TEST 4: Tai khoan khong ton tai

    @Test
    void checkout_userNotFound() {

        when(userRepository.findByEmailIgnoreCase(
                "unknown@gmail.com"))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> orderService.checkout(
                                "unknown@gmail.com",
                                form
                        )
                );

        assertTrue(
                exception.getMessage().contains("Không tìm thấy")
        );

        verify(cartRepository, never())
                .findByUser_UserId(anyInt());

        System.out.println("TEST 4 PASSED: Chan tai khoan khong ton tai");
    }

    // TEST 5: So dien thoai khong hop le

    @Test
    void checkout_invalidPhone() {

        form.setRecipientPhone("ABC123");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> orderService.checkout(
                                "customer@gmail.com",
                                form
                        )
                );

        assertTrue(
                exception.getMessage().contains("Số điện thoại")
        );

        verify(userRepository, never())
                .findByEmailIgnoreCase(anyString());

        System.out.println("TEST 5 PASSED: Chan so dien thoai sai");
    }

    // TEST 6: Phuong thuc thanh toan khong hop le

    @Test
    void checkout_invalidPaymentMethod() {

        form.setPaymentMethod("CASH");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> orderService.checkout(
                                "customer@gmail.com",
                                form
                        )
                );

        assertTrue(
                exception.getMessage().contains("không hợp lệ")
        );

        verify(userRepository, never())
                .findByEmailIgnoreCase(anyString());

        System.out.println("TEST 6 PASSED: Chan phuong thuc thanh toan sai");
    }

    // TEST 7: So luong khong hop le

    @Test
    void checkout_invalidQuantity() {

        cartItem.setQuantity(0);

        mockUserAndCart(List.of(cartItem));
        mockSaveOrder();

        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.checkout(
                        "customer@gmail.com",
                        form
                )
        );

        verify(entityManager, never())
                .find(
                        eq(ProductVariant.class),
                        any(),
                        any(LockModeType.class)
                );

        verify(orderItemRepository, never())
                .save(any(OrderItem.class));

        verify(cartItemRepository, never())
                .deleteByCart_CartId(anyLong());

        System.out.println("TEST 7 PASSED: Chan so luong khong hop le");
    }
}
