
package com.example.clothingshop;

import com.example.clothingshop.dto.CheckoutForm;
import com.example.clothingshop.entity.*;
import com.example.clothingshop.repository.*;
import com.example.clothingshop.service.OrderService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class OrderTransactionIntegrationTests {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ShopOrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final List<TestFixture> fixtures = new ArrayList<>();

    private TransactionTemplate transactionTemplate;

    private TransactionTemplate tx() {
        if (transactionTemplate == null) {
            transactionTemplate = new TransactionTemplate(transactionManager);
        }
        return transactionTemplate;
    }

    // Tao du lieu kiem thu rieng trong SQL Server

    private TestFixture createFixture(
            int numberOfUsers,
            int stock,
            int quantity
    ) {

        return tx().execute(status -> {

            String unique = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 12);

            Role customerRole = roleRepository
                    .findByRoleName("CUSTOMER")
                    .orElseGet(() -> {
                        Role role = new Role();
                        role.setRoleName("CUSTOMER");
                        return roleRepository.save(role);
                    });

            Category category = new Category();
            category.setCategoryName("TEST-" + unique);
            category.setDescription("Integration test category");
            category.setIsActive(true);
            category = categoryRepository.save(category);

            Product product = new Product();
            product.setCategory(category);
            product.setProductName("TEST-PRODUCT-" + unique);
            product.setDescription("Integration test product");
            product.setBasePrice(new BigDecimal("150000"));
            product.setIsActive(true);
            product.setCreatedAt(LocalDateTime.now());
            product = productRepository.save(product);

            ProductVariant variant = new ProductVariant();
            variant.setProduct(product);
            variant.setSku("TEST-" + unique);
            variant.setSize("L");
            variant.setColor("TEST");
            variant.setStockQuantity(stock);
            variant.setIsActive(true);
            variant = variantRepository.save(variant);

            TestFixture fixture = new TestFixture();

            fixture.category = category;
            fixture.product = product;
            fixture.variant = variant;

            for (int i = 0; i < numberOfUsers; i++) {

                User user = new User();
                user.setRole(customerRole);
                user.setFullName("Integration Customer " + i);
                user.setEmail(
                        "integration-" + unique + "-" + i + "@test.local"
                );
                user.setPhone("0901234567");
                user.setPasswordHash("integration-test-hash");
                user.setAddress("Integration Test Address");
                user.setIsActive(true);
                user.setCreatedAt(LocalDateTime.now());

                user = userRepository.save(user);

                Cart cart = new Cart();
                cart.setUser(user);
                cart.setUpdatedAt(LocalDateTime.now());
                cart = cartRepository.save(cart);

                CartItem cartItem = new CartItem();
                cartItem.setCart(cart);
                cartItem.setProductVariant(variant);
                cartItem.setQuantity(quantity);

                cartItemRepository.save(cartItem);

                fixture.users.add(user);
                fixture.carts.add(cart);
            }

            fixtures.add(fixture);

            return fixture;
        });
    }

    private CheckoutForm createCheckoutForm() {

        CheckoutForm form = new CheckoutForm();
        form.setRecipientName("Integration Test");
        form.setRecipientPhone("0901234567");
        form.setShippingAddress("Integration Test Address");
        form.setPaymentMethod("COD");
        form.setNote("Integration Test");

        return form;
    }

    // TEST 1: Rollback khi checkout that bai

    @Test
    void checkout_shouldRollbackWhenStockIsInsufficient() {

        TestFixture fixture = createFixture(1, 1, 2);

        User user = fixture.users.get(0);
        Cart cart = fixture.carts.get(0);

        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.checkout(
                        user.getEmail(),
                        createCheckoutForm()
                )
        );

        tx().executeWithoutResult(status -> {

            ProductVariant actualVariant =
                    variantRepository.findById(
                            fixture.variant.getVariantId()
                    ).orElseThrow();

            assertEquals(
                    1,
                    actualVariant.getStockQuantity(),
                    "Ton kho khong duoc bi tru khi checkout that bai"
            );

            assertTrue(
                    orderRepository
                            .findByUser_UserIdOrderByOrderDateDesc(
                                    user.getUserId()
                            )
                            .isEmpty(),
                    "Don hang phai duoc rollback"
            );

            assertTrue(
                    cartItemRepository.findByCart_CartId(
                            cart.getCartId()
                    ).size() == 1,
                    "Gio hang phai duoc giu lai khi checkout that bai"
            );
        });

        System.out.println(
                "TEST 1 PASSED: Rollback checkout khi khong du ton kho"
        );
    }

    // TEST 2: Hai khach cung mua san pham cuoi cung

    @Test
    void concurrentCheckout_shouldPreventOverselling()
            throws Exception {

        TestFixture fixture = createFixture(2, 1, 1);

        User user1 = fixture.users.get(0);
        User user2 = fixture.users.get(1);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {

            Future<String> customer1 = executor.submit(() -> {

                ready.countDown();
                start.await();

                try {
                    orderService.checkout(
                            user1.getEmail(),
                            createCheckoutForm()
                    );

                    return "SUCCESS";

                } catch (IllegalArgumentException exception) {

                    if (exception.getMessage() != null
                            && exception.getMessage().contains(
                            "không đủ số lượng")) {
                        return "OUT_OF_STOCK";
                    }

                    throw exception;
                }
            });

            Future<String> customer2 = executor.submit(() -> {

                ready.countDown();
                start.await();

                try {
                    orderService.checkout(
                            user2.getEmail(),
                            createCheckoutForm()
                    );

                    return "SUCCESS";

                } catch (IllegalArgumentException exception) {

                    if (exception.getMessage() != null
                            && exception.getMessage().contains(
                            "không đủ số lượng")) {
                        return "OUT_OF_STOCK";
                    }

                    throw exception;
                }
            });

            assertTrue(
                    ready.await(15, TimeUnit.SECONDS),
                    "Hai giao dich chua san sang"
            );

            // Cho hai giao dich bat dau gan nhu dong thoi
            start.countDown();

            String result1 = customer1.get(45, TimeUnit.SECONDS);
            String result2 = customer2.get(45, TimeUnit.SECONDS);

            int successCount = 0;
            int outOfStockCount = 0;

            if ("SUCCESS".equals(result1)) successCount++;
            if ("SUCCESS".equals(result2)) successCount++;

            if ("OUT_OF_STOCK".equals(result1)) outOfStockCount++;
            if ("OUT_OF_STOCK".equals(result2)) outOfStockCount++;

            assertEquals(
                    1,
                    successCount,
                    "Chi mot khach hang duoc dat hang thanh cong"
            );

            assertEquals(
                    1,
                    outOfStockCount,
                    "Khach con lai phai nhan thong bao het hang"
            );

            tx().executeWithoutResult(status -> {

                ProductVariant actualVariant =
                        variantRepository.findById(
                                fixture.variant.getVariantId()
                        ).orElseThrow();

                assertEquals(
                        0,
                        actualVariant.getStockQuantity(),
                        "Ton kho phai bang 0, khong duoc am"
                );

                long totalOrders =
                        orderRepository
                                .findByUser_UserIdOrderByOrderDateDesc(
                                        user1.getUserId()
                                ).size()
                                +
                                orderRepository
                                        .findByUser_UserIdOrderByOrderDateDesc(
                                                user2.getUserId()
                                        ).size();

                assertEquals(
                        1,
                        totalOrders,
                        "Chi duoc tao mot don hang"
                );
            });

            System.out.println(
                    "TEST 2 PASSED: Chan overselling khi hai khach mua dong thoi"
            );

        } finally {

            executor.shutdownNow();
        }
    }

    // Don dep du lieu test sau moi test

    @AfterEach
    void cleanUp() {

        if (fixtures.isEmpty()) {
            return;
        }

        tx().executeWithoutResult(status -> {

            for (TestFixture fixture : fixtures) {

                for (Cart cart : fixture.carts) {

                    cartItemRepository.deleteByCart_CartId(
                            cart.getCartId()
                    );
                }

                for (User user : fixture.users) {

                    List<ShopOrder> orders =
                            orderRepository
                                    .findByUser_UserIdOrderByOrderDateDesc(
                                            user.getUserId()
                                    );

                    for (ShopOrder order : orders) {

                        List<OrderItem> items =
                                orderItemRepository
                                        .findByShopOrder_OrderId(
                                                order.getOrderId()
                                        );

                        orderItemRepository.deleteAll(items);
                    }

                    orderRepository.deleteAll(orders);
                }

                cartRepository.deleteAll(fixture.carts);
                userRepository.deleteAll(fixture.users);

                variantRepository.delete(fixture.variant);
                productRepository.delete(fixture.product);
                categoryRepository.delete(fixture.category);
            }

            fixtures.clear();
        });
    }

    private static class TestFixture {

        private Category category;
        private Product product;
        private ProductVariant variant;

        private final List<User> users = new ArrayList<>();
        private final List<Cart> carts = new ArrayList<>();
    }
}
