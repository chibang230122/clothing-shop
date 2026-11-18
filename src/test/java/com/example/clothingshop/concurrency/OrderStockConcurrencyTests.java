package com.example.clothingshop.concurrency;

import com.example.clothingshop.dto.CheckoutForm;
import com.example.clothingshop.entity.*;
import com.example.clothingshop.network.ShopSocketServerLifecycle;
import com.example.clothingshop.repository.*;
import com.example.clothingshop.service.OrderCancellationService;
import com.example.clothingshop.service.OrderManagementService;
import com.example.clothingshop.service.OrderService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử tồn kho khi HỦY và XÁC NHẬN đơn hàng cùng lúc (thành viên 5, phối hợp thành viên 4),
 * đối chiếu kết quả qua Socket Server.
 *
 * Nguyên tắc kiểm tra ở mọi kịch bản (bảo toàn tồn kho):
 *   tồn kho còn lại = tồn kho ban đầu - tổng số lượng của các đơn CHƯA bị hủy
 * và tồn kho không bao giờ âm.
 *
 * Chạy trên SQL Server thật. Mỗi test tạo dữ liệu riêng (SKU và tài khoản ngẫu nhiên) và tự dọn.
 * Socket Server của test này dùng cổng ngẫu nhiên (shop.socket.port=0).
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "shop.socket.enabled=true",
                "shop.socket.port=0",
                "shop.socket.bind-address=127.0.0.1",
                "shop.socket.max-clients=20",
                "shop.socket.auth-token="
        })
class OrderStockConcurrencyTests {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String OUT_OF_STOCK_MESSAGE = "không đủ số lượng";

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderCancellationService orderCancellationService;

    @Autowired
    private OrderManagementService orderManagementService;

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

    @Autowired
    private ShopSocketServerLifecycle socketServer;

    private final List<TestFixture> fixtures = new ArrayList<>();

    private TransactionTemplate transactionTemplate;

    private TransactionTemplate tx() {
        if (transactionTemplate == null) {
            transactionTemplate = new TransactionTemplate(transactionManager);
        }
        return transactionTemplate;
    }

    // ===================================================================
    // TK-01: nhiều luồng cùng hủy MỘT đơn -> kho chỉ được hoàn đúng 1 lần
    // ===================================================================

    @Test
    void cancelSameOrderFromManyThreads_restoresStockExactlyOnce() throws Exception {
        int initialStock = 5;
        int quantity = 2;
        TestFixture fixture = createVariantFixture(initialStock);
        User customer = addUser(fixture, "CUSTOMER", quantity);

        ShopOrder order = orderService.checkout(customer.getEmail(), createCheckoutForm());
        Long orderId = order.getOrderId();
        assertEquals(initialStock - quantity, stockInDatabase(fixture));

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            tasks.add(() -> {
                orderCancellationService.cancelOrder(orderId, customer.getEmail());
                return null;
            });
        }
        List<Throwable> results = runTogether(tasks);

        assertEquals(1, countSuccess(results), "Chỉ 1 luồng được hủy thành công: " + describe(results));
        assertEquals(5, count(results, IllegalStateException.class),
                "5 luồng còn lại phải bị từ chối vì đơn đã hủy: " + describe(results));
        assertEquals(initialStock, stockInDatabase(fixture), "Kho chỉ được hoàn đúng 1 lần");
        assertEquals("CANCELLED", orderStatusInDatabase(orderId));
        assertStockConserved(fixture, initialStock);

        try (SocketTestClient client = new SocketTestClient(socketServer.getPort())) {
            assertEquals("CANCELLED", socketOrderStatus(client, order.getOrderCode()));
            assertEquals(initialStock, socketStock(client, fixture.variant.getSku()));
        }
    }

    // ===================================================================
    // TK-02: khách hủy và Staff xác nhận CÙNG MỘT đơn, cùng lúc
    // ===================================================================

    @Test
    void customerCancelVersusStaffConfirm_exactlyOneWins_andStockStaysConsistent() throws Exception {
        int rounds = 10;
        int initialStock = 20;
        TestFixture fixture = createVariantFixture(initialStock);

        List<User> customers = new ArrayList<>();
        List<ShopOrder> orders = new ArrayList<>();
        for (int i = 0; i < rounds; i++) {
            User customer = addUser(fixture, "CUSTOMER", 1);
            customers.add(customer);
            orders.add(orderService.checkout(customer.getEmail(), createCheckoutForm()));
        }
        assertEquals(initialStock - rounds, stockInDatabase(fixture));

        int cancelWins = 0;
        int confirmWins = 0;

        for (int i = 0; i < rounds; i++) {
            User customer = customers.get(i);
            Long orderId = orders.get(i).getOrderId();

            List<Callable<Void>> tasks = new ArrayList<>();
            tasks.add(() -> {                                    // khách hủy
                orderCancellationService.cancelOrder(orderId, customer.getEmail());
                return null;
            });
            tasks.add(() -> {                                    // staff xác nhận
                orderManagementService.updateOrderStatus(orderId, "CONFIRMED");
                return null;
            });
            List<Throwable> results = runTogether(tasks);
            Throwable cancelError = results.get(0);
            Throwable confirmError = results.get(1);

            assertEquals(1, countSuccess(results),
                    "Vòng " + i + ": đúng một bên được thành công: " + describe(results));

            String finalStatus = orderStatusInDatabase(orderId);
            if (cancelError == null) {
                cancelWins++;
                assertTrue(confirmError instanceof IllegalArgumentException,
                        "Xác nhận thua phải bị từ chối vì đơn đã hủy: " + describe(results));
                assertEquals("CANCELLED", finalStatus);
            } else {
                confirmWins++;
                assertTrue(cancelError instanceof IllegalStateException,
                        "Hủy thua phải bị từ chối vì đơn đã được xác nhận: " + describe(results));
                assertEquals("CONFIRMED", finalStatus);
            }
        }

        System.out.println("[TEST] TK-02: khách hủy thắng " + cancelWins + " vòng, staff xác nhận thắng "
                + confirmWins + " vòng (cả hai kết quả đều hợp lệ)");

        // Kho chỉ được hoàn cho các đơn bị hủy
        assertEquals(initialStock - confirmWins, stockInDatabase(fixture));
        assertStockConserved(fixture, initialStock);

        // Socket phải khớp database cho từng đơn
        try (SocketTestClient client = new SocketTestClient(socketServer.getPort())) {
            for (ShopOrder order : orders) {
                assertEquals(orderStatusInDatabase(order.getOrderId()),
                        socketOrderStatus(client, order.getOrderCode()));
            }
            assertEquals(initialStock - confirmWins, socketStock(client, fixture.variant.getSku()));
        }
    }

    // ===================================================================
    // TK-03: hủy đơn trong lúc người khác đang mua đúng món vừa được hoàn
    // ===================================================================

    @Test
    void cancelWhileOthersBuyTheSameItem_stockNeverNegative_andConserved() throws Exception {
        int rounds = 5;
        int initialStock = 2;

        for (int round = 0; round < rounds; round++) {
            TestFixture fixture = createVariantFixture(initialStock);
            User buyerA = addUser(fixture, "CUSTOMER", 2);       // mua hết kho
            User buyerB = addUser(fixture, "CUSTOMER", 1);
            User buyerC = addUser(fixture, "CUSTOMER", 1);

            ShopOrder orderA = orderService.checkout(buyerA.getEmail(), createCheckoutForm());
            assertEquals(0, stockInDatabase(fixture));

            List<Callable<Void>> tasks = new ArrayList<>();
            tasks.add(() -> {                                    // A hủy, kho được hoàn 2
                orderCancellationService.cancelOrder(orderA.getOrderId(), buyerA.getEmail());
                return null;
            });
            tasks.add(() -> {
                orderService.checkout(buyerB.getEmail(), createCheckoutForm());
                return null;
            });
            tasks.add(() -> {
                orderService.checkout(buyerC.getEmail(), createCheckoutForm());
                return null;
            });
            List<Throwable> results = runTogether(tasks);

            assertNull(results.get(0), "Việc hủy đơn của A phải thành công: " + describe(results));
            int bcSuccess = 0;
            for (int i = 1; i <= 2; i++) {
                Throwable error = results.get(i);
                if (error == null) {
                    bcSuccess++;
                } else {
                    assertTrue(error instanceof IllegalArgumentException
                                    && String.valueOf(error.getMessage()).contains(OUT_OF_STOCK_MESSAGE),
                            "Khách B/C chỉ được phép bị từ chối vì hết hàng, nhưng gặp: " + describe(results));
                }
            }

            int finalStock = stockInDatabase(fixture);
            assertTrue(finalStock >= 0, "Tồn kho không được âm: " + finalStock);
            assertEquals(initialStock - bcSuccess, finalStock);
            assertStockConserved(fixture, initialStock);

            try (SocketTestClient client = new SocketTestClient(socketServer.getPort())) {
                assertEquals(finalStock, socketStock(client, fixture.variant.getSku()));
                assertEquals("CANCELLED", socketOrderStatus(client, orderA.getOrderCode()));
            }
        }
    }

    // ===================================================================
    // Việc 4: trạng thái đơn qua Socket đi theo quy trình của Staff
    // ===================================================================

    @Test
    void socketOrderStatus_followsStaffWorkflow_andLaterCancelIsRejected() throws Exception {
        int initialStock = 4;
        TestFixture fixture = createVariantFixture(initialStock);
        User customer = addUser(fixture, "CUSTOMER", 1);
        ShopOrder order = orderService.checkout(customer.getEmail(), createCheckoutForm());
        Long orderId = order.getOrderId();

        try (SocketTestClient client = new SocketTestClient(socketServer.getPort())) {
            assertEquals("PENDING", socketOrderStatus(client, order.getOrderCode()));

            for (String next : new String[]{"CONFIRMED", "PROCESSING", "SHIPPING", "COMPLETED"}) {
                orderManagementService.updateOrderStatus(orderId, next);
                assertEquals(next, socketOrderStatus(client, order.getOrderCode()));
            }

            // Đã hoàn thành thì khách không thể hủy, kho không đổi
            assertThrows(IllegalStateException.class,
                    () -> orderCancellationService.cancelOrder(orderId, customer.getEmail()));
            assertEquals(initialStock - 1, stockInDatabase(fixture));
            assertEquals(initialStock - 1, socketStock(client, fixture.variant.getSku()));
        }
        assertStockConserved(fixture, initialStock);
    }

    // ===================================================================
    // Việc 4: ADMIN và STAFF không được đặt hàng; kho không đổi
    // ===================================================================

    @Test
    void adminAndStaffCannotCheckout_stockUnchanged() throws Exception {
        int initialStock = 3;
        TestFixture fixture = createVariantFixture(initialStock);

        for (String roleName : new String[]{"STAFF", "ADMIN"}) {
            User user = addUser(fixture, roleName, 1);
            assertThrows(AccessDeniedException.class,
                    () -> orderService.checkout(user.getEmail(), createCheckoutForm()),
                    "Vai trò " + roleName + " phải bị chặn đặt hàng");
            assertEquals(0, orderRepository.findByUser_UserIdOrderByOrderDateDesc(user.getUserId()).size());
        }

        assertEquals(initialStock, stockInDatabase(fixture));
        try (SocketTestClient client = new SocketTestClient(socketServer.getPort())) {
            assertEquals(initialStock, socketStock(client, fixture.variant.getSku()));
        }
    }

    // ===================================================================
    // Hỗ trợ: chạy nhiều việc cùng lúc
    // ===================================================================

    /**
     * Thả tất cả công việc chạy cùng một lúc (đợi đủ luồng sẵn sàng rồi mới cho chạy).
     * Trả về danh sách lỗi theo đúng thứ tự công việc; phần tử null nghĩa là thành công.
     */
    private List<Throwable> runTogether(List<Callable<Void>> tasks) throws Exception {
        int count = tasks.size();
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(count, namedFactory("race-"));
        try {
            List<Future<Throwable>> futures = new ArrayList<>();
            for (Callable<Void> task : tasks) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    go.await();
                    try {
                        task.call();
                        return null;
                    } catch (Throwable error) {
                        return error;
                    }
                }));
            }
            assertTrue(ready.await(30, TimeUnit.SECONDS), "Các luồng chưa sẵn sàng");
            go.countDown();

            List<Throwable> results = new ArrayList<>();
            for (Future<Throwable> future : futures) {
                results.add(future.get(90, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    private static long countSuccess(List<Throwable> results) {
        return results.stream().filter(error -> error == null).count();
    }

    private static long count(List<Throwable> results, Class<? extends Throwable> type) {
        return results.stream().filter(type::isInstance).count();
    }

    private static String describe(List<Throwable> results) {
        List<String> names = new ArrayList<>();
        for (Throwable error : results) {
            names.add(error == null ? "OK" : error.getClass().getSimpleName() + "(" + error.getMessage() + ")");
        }
        return names.toString();
    }

    private static ThreadFactory namedFactory(String prefix) {
        AtomicInteger counter = new AtomicInteger(1);
        return runnable -> new Thread(runnable, prefix + counter.getAndIncrement());
    }

    // ===================================================================
    // Hỗ trợ: đọc database và Socket
    // ===================================================================

    private int stockInDatabase(TestFixture fixture) {
        return tx().execute(status -> variantRepository
                .findById(fixture.variant.getVariantId())
                .orElseThrow()
                .getStockQuantity());
    }

    private String orderStatusInDatabase(Long orderId) {
        return tx().execute(status -> orderRepository.findById(orderId).orElseThrow().getOrderStatus());
    }

    /** Tổng số lượng của các đơn chưa bị hủy thuộc các khách trong fixture. */
    private int soldQuantity(TestFixture fixture) {
        return tx().execute(status -> {
            int total = 0;
            for (User user : fixture.users) {
                for (ShopOrder order : orderRepository.findByUser_UserIdOrderByOrderDateDesc(user.getUserId())) {
                    if ("CANCELLED".equals(order.getOrderStatus())) {
                        continue;
                    }
                    for (OrderItem item : orderItemRepository.findByShopOrder_OrderId(order.getOrderId())) {
                        total += item.getQuantity();
                    }
                }
            }
            return total;
        });
    }

    /** Bảo toàn tồn kho: còn lại = ban đầu - số lượng của các đơn chưa hủy, và không âm. */
    private void assertStockConserved(TestFixture fixture, int initialStock) {
        int stock = stockInDatabase(fixture);
        assertTrue(stock >= 0, "Tồn kho không được âm: " + stock);
        assertEquals(initialStock - soldQuantity(fixture), stock,
                "Tồn kho phải bằng ban đầu trừ số lượng của các đơn chưa hủy");
    }

    private int socketStock(SocketTestClient client, String sku) throws IOException {
        JsonNode response = MAPPER.readTree(client.request("CHECK_STOCK " + sku));
        assertEquals("OK", response.path("status").asText(), response.toString());
        return response.path("data").path("stockQuantity").asInt(-1);
    }

    private String socketOrderStatus(SocketTestClient client, String orderCode) throws IOException {
        JsonNode response = MAPPER.readTree(client.request("GET_ORDER_STATUS " + orderCode));
        assertEquals("OK", response.path("status").asText(), response.toString());
        return response.path("data").path("orderStatus").asText();
    }

    /** Client Socket đơn giản cho test: gửi một dòng, đọc một dòng (UTF-8). */
    private static final class SocketTestClient implements AutoCloseable {
        private final Socket socket = new Socket();
        private final BufferedReader in;
        private final PrintWriter out;

        SocketTestClient(int port) throws IOException {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 5000);
            socket.setSoTimeout(60_000);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        }

        String request(String line) throws IOException {
            out.println(line);
            String response = in.readLine();
            if (response == null) {
                throw new IOException("Server đã đóng kết nối");
            }
            return response;
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
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

    // ===================================================================
    // Tạo dữ liệu kiểm thử riêng (cùng cách làm với OrderTransactionIntegrationTests)
    // ===================================================================

    /** Tạo danh mục, sản phẩm và một biến thể với số lượng tồn cho trước. */
    private TestFixture createVariantFixture(int stock) {
        return tx().execute(status -> {

            String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

            Category category = new Category();
            category.setCategoryName("TEST-" + unique);
            category.setDescription("Stock concurrency test category");
            category.setIsActive(true);
            category = categoryRepository.save(category);

            Product product = new Product();
            product.setCategory(category);
            product.setProductName("TEST-PRODUCT-" + unique);
            product.setDescription("Stock concurrency test product");
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
            fixture.unique = unique;
            fixture.category = category;
            fixture.product = product;
            fixture.variant = variant;
            fixtures.add(fixture);
            return fixture;
        });
    }

    /** Thêm một tài khoản (vai trò cho trước) cùng giỏ hàng chứa "quantity" sản phẩm của fixture. */
    private User addUser(TestFixture fixture, String roleName, int quantity) {
        return tx().execute(status -> {

            Role role = roleRepository
                    .findByRoleName(roleName)
                    .orElseGet(() -> {
                        Role created = new Role();
                        created.setRoleName(roleName);
                        return roleRepository.save(created);
                    });

            int index = fixture.users.size();

            User user = new User();
            user.setRole(role);
            user.setFullName("Stock Test " + roleName + " " + index);
            user.setEmail("stock-" + fixture.unique + "-" + index + "@test.local");
            user.setPhone("0901234567");
            user.setPasswordHash("stock-test-hash");
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
            cartItem.setProductVariant(fixture.variant);
            cartItem.setQuantity(quantity);
            cartItemRepository.save(cartItem);

            fixture.users.add(user);
            fixture.carts.add(cart);
            return user;
        });
    }

    // Dọn dữ liệu test sau mỗi test

    @AfterEach
    void cleanUp() {

        if (fixtures.isEmpty()) {
            return;
        }

        tx().executeWithoutResult(status -> {

            for (TestFixture fixture : fixtures) {

                for (Cart cart : fixture.carts) {
                    cartItemRepository.deleteByCart_CartId(cart.getCartId());
                }

                for (User user : fixture.users) {

                    List<ShopOrder> orders = orderRepository
                            .findByUser_UserIdOrderByOrderDateDesc(user.getUserId());

                    for (ShopOrder order : orders) {
                        List<OrderItem> items = orderItemRepository
                                .findByShopOrder_OrderId(order.getOrderId());
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

        private String unique;
        private Category category;
        private Product product;
        private ProductVariant variant;

        private final List<User> users = new ArrayList<>();
        private final List<Cart> carts = new ArrayList<>();
    }
}
