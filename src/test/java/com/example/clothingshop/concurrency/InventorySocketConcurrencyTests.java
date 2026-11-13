package com.example.clothingshop.concurrency;

import com.example.clothingshop.dto.CheckoutForm;
import com.example.clothingshop.entity.*;
import com.example.clothingshop.network.ShopSocketServerLifecycle;
import com.example.clothingshop.repository.*;
import com.example.clothingshop.service.OrderService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử đa luồng + mạng trên SQL Server thật (thành viên 5):
 * nhiều khách mua tranh nhau số hàng còn lại trong khi các client Socket liên tục hỏi tồn kho.
 *
 * Mỗi test tạo dữ liệu riêng (SKU và tài khoản ngẫu nhiên) và tự dọn sau khi chạy.
 * Socket server của test này dùng cổng ngẫu nhiên (shop.socket.port=0) nên không đụng ứng dụng đang chạy.
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
class InventorySocketConcurrencyTests {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String OUT_OF_STOCK_MESSAGE = "không đủ số lượng";

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
    // KỊCH BẢN 1: 3 khách cùng mua, kho chỉ còn 2 -> đúng 2 thành công
    // ===================================================================

    @Test
    void threeCustomersBuyLastTwoItems_exactlyTwoSucceed_andSocketReportsZeroStock() throws Exception {
        TestFixture fixture = createFixture(3, 2, 1);
        String sku = fixture.variant.getSku();

        List<String> results = runConcurrentCheckouts(fixture, 3);

        assertEquals(2, count(results, "SUCCESS"), "Kết quả: " + results);
        assertEquals(1, count(results, "OUT_OF_STOCK"), "Kết quả: " + results);
        assertEquals(0, stockInDatabase(fixture), "Tồn kho trong database không được âm hay còn dư");
        assertEquals(2, countOrders(fixture), "Chỉ được tạo đúng 2 đơn hàng");

        try (SocketTestClient client = new SocketTestClient(socketServer.getPort())) {
            JsonNode response = MAPPER.readTree(client.request("CHECK_STOCK " + sku));
            assertEquals("OK", response.path("status").asText(), response.toString());
            assertEquals(0, response.path("data").path("stockQuantity").asInt(-1), response.toString());
            assertFalse(response.path("data").path("available").asBoolean(true), response.toString());
        }
    }

    // ===================================================================
    // KỊCH BẢN 2: 20 khách tranh 5 sản phẩm, đồng thời 1 client Socket liên tục hỏi tồn kho
    // ===================================================================

    @Test
    void twentyCustomersCompeteForFiveItems_neverOversell_andSocketNeverSeesInconsistentStock() throws Exception {
        int initialStock = 5;
        TestFixture fixture = createFixture(20, initialStock, 1);
        String sku = fixture.variant.getSku();

        List<Integer> observed = Collections.synchronizedList(new ArrayList<>());
        List<String> violations = Collections.synchronizedList(new ArrayList<>());
        AtomicBoolean monitoring = new AtomicBoolean(true);

        // Client Socket quan sát: đọc tồn kho liên tục trong lúc các đơn hàng đang được xử lý
        ExecutorService monitorExecutor = Executors.newSingleThreadExecutor(namedFactory("socket-monitor-"));
        Future<?> monitor = monitorExecutor.submit(() -> {
            try (SocketTestClient client = new SocketTestClient(socketServer.getPort())) {
                int previous = initialStock;
                do {
                    JsonNode response = MAPPER.readTree(client.request("CHECK_STOCK " + sku));
                    int stock = response.path("data").path("stockQuantity").asInt(-1);
                    observed.add(stock);
                    if (stock < 0 || stock > initialStock) {
                        violations.add("Tồn kho ngoài khoảng cho phép: " + stock);
                    }
                    if (stock > previous) {
                        violations.add("Tồn kho tăng bất thường từ " + previous + " lên " + stock);
                    }
                    previous = stock;
                    Thread.sleep(10);
                } while (monitoring.get());
            }
            return null;
        });

        List<String> results;
        try {
            Thread.sleep(200);          // để client quan sát có vài lần đọc trước khi bắt đầu mua
            results = runConcurrentCheckouts(fixture, 10);
        } finally {
            monitoring.set(false);
        }
        monitor.get(30, TimeUnit.SECONDS);
        monitorExecutor.shutdownNow();

        assertEquals(initialStock, count(results, "SUCCESS"), "Kết quả: " + results);
        assertEquals(15, count(results, "OUT_OF_STOCK"), "Kết quả: " + results);
        assertEquals(0, stockInDatabase(fixture));
        assertEquals(initialStock, countOrders(fixture));
        assertTrue(violations.isEmpty(), "Client Socket đọc được dữ liệu sai: " + violations);
        assertFalse(observed.isEmpty(), "Client Socket phải đọc được ít nhất một lần");
        assertEquals(0, (int) observed.get(observed.size() - 1), "Lần đọc cuối phải là hết hàng: " + observed);

        System.out.println("[TEST] Client Socket đã quan sát " + observed.size()
                + " lần, chuỗi tồn kho: " + observed);
    }

    // ===================================================================
    // KỊCH BẢN 3: Lệnh Socket phản ánh đúng database sau khi đặt hàng
    // ===================================================================

    @Test
    void socketCommands_reflectDatabase_afterCheckout() throws Exception {
        TestFixture fixture = createFixture(1, 3, 1);
        User user = fixture.users.get(0);
        String sku = fixture.variant.getSku();

        ShopOrder order = orderService.checkout(user.getEmail(), createCheckoutForm());

        try (SocketTestClient client = new SocketTestClient(socketServer.getPort())) {
            JsonNode stock = MAPPER.readTree(client.request("CHECK_STOCK " + sku.toLowerCase()));
            assertEquals("OK", stock.path("status").asText(), stock.toString());
            assertEquals(2, stock.path("data").path("stockQuantity").asInt(-1), stock.toString());
            assertTrue(stock.path("data").path("available").asBoolean(false), stock.toString());

            JsonNode status = MAPPER.readTree(client.request("GET_ORDER_STATUS " + order.getOrderCode()));
            assertEquals("OK", status.path("status").asText(), status.toString());
            assertEquals("PENDING", status.path("data").path("orderStatus").asText(), status.toString());
            assertEquals("UNPAID", status.path("data").path("paymentStatus").asText(), status.toString());
            assertFalse(status.toString().contains(user.getEmail()), "Không được trả thông tin cá nhân");

            JsonNode missingSku = MAPPER.readTree(client.request("CHECK_STOCK KHONG-TON-TAI-123"));
            assertEquals("NOT_FOUND", missingSku.path("code").asText(), missingSku.toString());

            JsonNode missingOrder = MAPPER.readTree(client.request("GET_ORDER_STATUS DH-KHONG-CO"));
            assertEquals("NOT_FOUND", missingOrder.path("code").asText(), missingOrder.toString());
        }
    }

    // ===================================================================
    // KỊCH BẢN 4: Nhiều client Socket cùng lúc, mỗi client nhiều yêu cầu, đọc từ database thật
    // ===================================================================

    @Test
    void manySocketClientsAtOnce_allGetCorrectStockFromDatabase() throws Exception {
        TestFixture fixture = createFixture(1, 7, 1);
        String sku = fixture.variant.getSku();
        int clients = 10;
        int requestsPerClient = 20;
        AtomicInteger correct = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(clients, namedFactory("socket-client-"));
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < clients; i++) {
                futures.add(pool.submit(() -> {
                    try (SocketTestClient client = new SocketTestClient(socketServer.getPort())) {
                        for (int r = 0; r < requestsPerClient; r++) {
                            JsonNode response = MAPPER.readTree(client.request("CHECK_STOCK " + sku));
                            if (response.path("data").path("stockQuantity").asInt(-1) == 7) {
                                correct.incrementAndGet();
                            }
                        }
                    }
                    return null;
                }));
            }
            for (Future<?> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
        assertEquals(clients * requestsPerClient, correct.get());
    }

    // ===================================================================
    // Hỗ trợ
    // ===================================================================

    /** Cho mọi khách trong fixture checkout cùng một lúc trên tối đa "threads" luồng. */
    private List<String> runConcurrentCheckouts(TestFixture fixture, int threads) throws Exception {
        int customers = fixture.users.size();
        CountDownLatch ready = new CountDownLatch(Math.min(threads, customers));
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threads, namedFactory("checkout-"));
        try {
            List<Future<String>> futures = new ArrayList<>();
            for (User user : fixture.users) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return checkoutOnce(user);
                }));
            }
            assertTrue(ready.await(30, TimeUnit.SECONDS), "Các luồng chưa sẵn sàng");
            start.countDown();                    // tất cả bắt đầu cùng lúc

            List<String> results = new ArrayList<>();
            for (Future<String> future : futures) {
                results.add(future.get(90, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    private String checkoutOnce(User user) {
        String thread = Thread.currentThread().getName();
        System.out.printf("[%s] %s START checkout (%s)%n", LocalDateTime.now(), thread, user.getEmail());
        try {
            orderService.checkout(user.getEmail(), createCheckoutForm());
            System.out.printf("[%s] %s SUCCESS%n", LocalDateTime.now(), thread);
            return "SUCCESS";
        } catch (IllegalArgumentException exception) {
            if (exception.getMessage() != null && exception.getMessage().contains(OUT_OF_STOCK_MESSAGE)) {
                System.out.printf("[%s] %s OUT_OF_STOCK%n", LocalDateTime.now(), thread);
                return "OUT_OF_STOCK";
            }
            throw exception;
        }
    }

    private static long count(List<String> results, String value) {
        return results.stream().filter(value::equals).count();
    }

    private int stockInDatabase(TestFixture fixture) {
        return tx().execute(status -> variantRepository
                .findById(fixture.variant.getVariantId())
                .orElseThrow()
                .getStockQuantity());
    }

    private int countOrders(TestFixture fixture) {
        return tx().execute(status -> {
            int total = 0;
            for (User user : fixture.users) {
                total += orderRepository.findByUser_UserIdOrderByOrderDateDesc(user.getUserId()).size();
            }
            return total;
        });
    }

    private static ThreadFactory namedFactory(String prefix) {
        AtomicInteger counter = new AtomicInteger(1);
        return runnable -> new Thread(runnable, prefix + counter.getAndIncrement());
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

    /** Client Socket đơn giản cho test: gửi một dòng, đọc một dòng (UTF-8). */
    private static final class SocketTestClient implements AutoCloseable {
        private final Socket socket = new Socket();
        private final BufferedReader in;
        private final PrintWriter out;

        SocketTestClient(int port) throws IOException {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 5000);
            socket.setSoTimeout(60_000);   // đọc có thể phải chờ khóa của giao dịch đặt hàng đang chạy
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

    // Tạo dữ liệu kiểm thử riêng trong SQL Server (cùng cách làm với OrderTransactionIntegrationTests)

    private TestFixture createFixture(int numberOfUsers, int stock, int quantity) {

        return tx().execute(status -> {

            String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

            Role customerRole = roleRepository
                    .findByRoleName("CUSTOMER")
                    .orElseGet(() -> {
                        Role role = new Role();
                        role.setRoleName("CUSTOMER");
                        return roleRepository.save(role);
                    });

            Category category = new Category();
            category.setCategoryName("TEST-" + unique);
            category.setDescription("Socket concurrency test category");
            category.setIsActive(true);
            category = categoryRepository.save(category);

            Product product = new Product();
            product.setCategory(category);
            product.setProductName("TEST-PRODUCT-" + unique);
            product.setDescription("Socket concurrency test product");
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
                user.setFullName("Socket Test Customer " + i);
                user.setEmail("socket-" + unique + "-" + i + "@test.local");
                user.setPhone("0901234567");
                user.setPasswordHash("socket-test-hash");
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

        private Category category;
        private Product product;
        private ProductVariant variant;

        private final List<User> users = new ArrayList<>();
        private final List<Cart> carts = new ArrayList<>();
    }
}
