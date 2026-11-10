package com.example.clothingshop.network;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử Socket Server bằng socket thật trên localhost, dùng dữ liệu giả cho tồn kho/đơn hàng nên
 * KHÔNG cần SQL Server. Các kịch bản tích hợp với database nằm ở InventorySocketConcurrencyTests.
 */
class ShopSocketServerTests {

    private static final StockInfo SAMPLE_STOCK =
            new StockInfo("AT-DEN-M", "Áo thun đen", "M", "Đen", 2, true);

    private ShopSocketServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    // ===== Tiện ích =====

    private static SocketServerSettings settings(int maxClients, int idleMillis) {
        return new SocketServerSettings(0, "127.0.0.1", maxClients, idleMillis, 128);
    }

    private ShopSocketServer startServer(SocketServerSettings settings,
                                         StockLookup stock,
                                         OrderStatusLookup orders,
                                         String token) throws IOException {
        server = new ShopSocketServer(settings, new CommandProcessor(stock, orders, token));
        server.start();
        return server;
    }

    private ShopSocketServer startDefaultServer() throws IOException {
        StockLookup stock = sku -> "AT-DEN-M".equalsIgnoreCase(sku) ? Optional.of(SAMPLE_STOCK) : Optional.empty();
        OrderStatusLookup orders = code -> "DH123".equals(code)
                ? Optional.of(new OrderStatusInfo("DH123", "PENDING", "UNPAID", "2026-01-01T10:00:00"))
                : Optional.empty();
        return startServer(settings(10, 5000), stock, orders, "");
    }

    /** Client thử nghiệm: gửi một dòng, đọc một dòng. */
    private static final class TestClient implements AutoCloseable {
        final Socket socket = new Socket();
        final BufferedReader in;
        final OutputStream out;

        TestClient(int port) throws IOException {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 3000);
            socket.setSoTimeout(5000);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = socket.getOutputStream();
        }

        void sendRaw(String text) throws IOException {
            out.write(text.getBytes(StandardCharsets.UTF_8));
            out.flush();
        }

        String request(String line) throws IOException {
            sendRaw(line + "\n");
            return in.readLine();
        }

        String readLine() throws IOException {
            return in.readLine();
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }

    private static void waitUntil(java.util.function.BooleanSupplier condition, long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
    }

    // ===== Các lệnh cơ bản =====

    @Test
    void ping_returnsPong() throws Exception {
        startDefaultServer();
        try (TestClient c = new TestClient(server.getPort())) {
            String response = c.request("PING");
            assertTrue(response.contains("\"status\":\"OK\""), response);
            assertTrue(response.contains("\"message\":\"PONG\""), response);
            assertTrue(response.contains("\"handledBy\":\"shop-socket-worker-"), response);
        }
    }

    @Test
    void commandIsCaseInsensitive() throws Exception {
        startDefaultServer();
        try (TestClient c = new TestClient(server.getPort())) {
            assertTrue(c.request("ping").contains("PONG"));
        }
    }

    @Test
    void checkStock_returnsDataIncludingVietnameseText() throws Exception {
        startDefaultServer();
        try (TestClient c = new TestClient(server.getPort())) {
            String response = c.request("CHECK_STOCK AT-DEN-M");
            assertTrue(response.contains("\"status\":\"OK\""), response);
            assertTrue(response.contains("\"sku\":\"AT-DEN-M\""), response);
            assertTrue(response.contains("\"stockQuantity\":2"), response);
            assertTrue(response.contains("\"available\":true"), response);
            assertTrue(response.contains("Áo thun đen"), response);
        }
    }

    @Test
    void checkStock_unknownSku_returnsNotFound() throws Exception {
        startDefaultServer();
        try (TestClient c = new TestClient(server.getPort())) {
            String response = c.request("CHECK_STOCK KHONG-TON-TAI");
            assertTrue(response.contains("\"status\":\"ERROR\""), response);
            assertTrue(response.contains("\"code\":\"NOT_FOUND\""), response);
        }
    }

    @Test
    void getOrderStatus_returnsStatusWithoutPersonalData() throws Exception {
        startDefaultServer();
        try (TestClient c = new TestClient(server.getPort())) {
            String response = c.request("GET_ORDER_STATUS DH123");
            assertTrue(response.contains("\"orderStatus\":\"PENDING\""), response);
            assertTrue(response.contains("\"paymentStatus\":\"UNPAID\""), response);
            assertFalse(response.contains("recipient"), response);
            assertTrue(c.request("GET_ORDER_STATUS DH999").contains("NOT_FOUND"));
        }
    }

    @Test
    void quit_closesConnection() throws Exception {
        startDefaultServer();
        try (TestClient c = new TestClient(server.getPort())) {
            assertTrue(c.request("QUIT").contains("BYE"));
            assertNull(c.readLine());
        }
    }

    // ===== Dữ liệu sai =====

    @Test
    void unknownCommand_returnsErrorAndConnectionStaysOpen() throws Exception {
        startDefaultServer();
        try (TestClient c = new TestClient(server.getPort())) {
            assertTrue(c.request("DROP TABLE Users").contains("UNKNOWN_COMMAND"));
            assertTrue(c.request("PING").contains("PONG"));
        }
    }

    @Test
    void invalidArguments_areRejectedBeforeTouchingTheDatabase() throws Exception {
        AtomicInteger lookups = new AtomicInteger();
        StockLookup stock = sku -> {
            lookups.incrementAndGet();
            return Optional.empty();
        };
        startServer(settings(5, 5000), stock, code -> Optional.empty(), "");
        try (TestClient c = new TestClient(server.getPort())) {
            assertTrue(c.request("CHECK_STOCK").contains("INVALID_ARGUMENT"));
            assertTrue(c.request("CHECK_STOCK A B").contains("INVALID_ARGUMENT"));
            assertTrue(c.request("CHECK_STOCK ../../etc/passwd").contains("INVALID_ARGUMENT"));
            assertTrue(c.request("CHECK_STOCK ' OR 1=1 --").contains("INVALID_ARGUMENT"));
            assertTrue(c.request("CHECK_STOCK " + "A".repeat(60)).contains("INVALID_ARGUMENT"));
            assertTrue(c.request("PING extra").contains("INVALID_ARGUMENT"));
        }
        assertEquals(0, lookups.get());
    }

    @Test
    void emptyLine_returnsEmptyRequest() throws Exception {
        startDefaultServer();
        try (TestClient c = new TestClient(server.getPort())) {
            assertTrue(c.request("").contains("EMPTY_REQUEST"));
            assertTrue(c.request("PING").contains("PONG"));
        }
    }

    @Test
    void controlCharacters_areRejected() throws Exception {
        startDefaultServer();
        try (TestClient c = new TestClient(server.getPort())) {
            assertTrue(c.request("CHECK_STOCK\u0001AT-DEN-M").contains("INVALID_REQUEST"));
        }
    }

    @Test
    void oversizeLine_returnsErrorAndClosesConnection() throws Exception {
        startDefaultServer();
        try (TestClient c = new TestClient(server.getPort())) {
            String response = c.request("A".repeat(400));
            assertTrue(response.contains("LINE_TOO_LONG"), response);
            assertNull(c.readLine());
        }
        // Server vẫn phục vụ client khác
        try (TestClient c2 = new TestClient(server.getPort())) {
            assertTrue(c2.request("PING").contains("PONG"));
        }
    }

    @Test
    void lookupThrowsException_clientGetsInternalErrorWithoutDetails() throws Exception {
        StockLookup broken = sku -> {
            throw new IllegalStateException("boom-secret-sql-detail");
        };
        startServer(settings(5, 5000), broken, code -> Optional.empty(), "");
        try (TestClient c = new TestClient(server.getPort())) {
            String response = c.request("CHECK_STOCK AT-DEN-M");
            assertTrue(response.contains("INTERNAL_ERROR"), response);
            assertFalse(response.contains("boom-secret-sql-detail"), response);
            assertTrue(c.request("PING").contains("PONG"));
        }
    }

    // ===== Xác thực =====

    @Test
    void authToken_isRequiredForBusinessCommands() throws Exception {
        StockLookup stock = sku -> Optional.of(SAMPLE_STOCK);
        startServer(settings(5, 5000), stock, code -> Optional.empty(), "s3cret-token");
        try (TestClient c = new TestClient(server.getPort())) {
            assertTrue(c.request("PING").contains("PONG"));
            assertTrue(c.request("CHECK_STOCK AT-DEN-M").contains("UNAUTHORIZED"));
            assertTrue(c.request("AUTH wrong").contains("AUTH_FAILED"));
            assertTrue(c.request("CHECK_STOCK AT-DEN-M").contains("UNAUTHORIZED"));
            assertTrue(c.request("AUTH s3cret-token").contains("\"status\":\"OK\""));
            assertTrue(c.request("CHECK_STOCK AT-DEN-M").contains("\"sku\":\"AT-DEN-M\""));
        }
        // Kết nối mới phải AUTH lại
        try (TestClient c2 = new TestClient(server.getPort())) {
            assertTrue(c2.request("CHECK_STOCK AT-DEN-M").contains("UNAUTHORIZED"));
        }
    }

    // ===== Đồng thời =====

    @Test
    void threeClientsAtTheSameTime_areServedConcurrentlyOnDifferentThreads() throws Exception {
        // Mỗi lần tra cứu sẽ chờ đến khi đủ 3 luồng cùng vào. Nếu server phục vụ tuần tự từng client thì
        // sẽ không bao giờ đủ 3 luồng và test thất bại do quá thời gian chờ.
        CountDownLatch allThreeInside = new CountDownLatch(3);
        StockLookup stock = sku -> {
            allThreeInside.countDown();
            try {
                if (!allThreeInside.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Server không xử lý đồng thời 3 client");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return Optional.of(SAMPLE_STOCK);
        };
        startServer(settings(10, 10000), stock, code -> Optional.empty(), "");

        ExecutorService clients = Executors.newFixedThreadPool(3);
        try {
            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                futures.add(clients.submit(() -> {
                    try (TestClient c = new TestClient(server.getPort())) {
                        return c.request("CHECK_STOCK AT-DEN-M");
                    }
                }));
            }
            Set<String> workerThreads = new HashSet<>();
            for (Future<String> f : futures) {
                String response = f.get(15, TimeUnit.SECONDS);
                assertTrue(response.contains("\"status\":\"OK\""), response);
                int from = response.indexOf("\"handledBy\":\"") + 13;
                workerThreads.add(response.substring(from, response.indexOf('"', from)));
            }
            assertEquals(3, workerThreads.size(), "3 client phải được 3 luồng khác nhau phục vụ: " + workerThreads);
        } finally {
            clients.shutdownNow();
        }
    }

    @Test
    void manyClientsSendingManyRequests_allGetAnswers() throws Exception {
        startServer(settings(50, 10000), sku -> Optional.of(SAMPLE_STOCK), code -> Optional.empty(), "");
        int clientCount = 30;
        int requestsPerClient = 25;
        AtomicInteger okCount = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(clientCount);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < clientCount; i++) {
                futures.add(pool.submit(() -> {
                    try (TestClient c = new TestClient(server.getPort())) {
                        for (int r = 0; r < requestsPerClient; r++) {
                            if (c.request("CHECK_STOCK AT-DEN-M").contains("\"stockQuantity\":2")) {
                                okCount.incrementAndGet();
                            }
                        }
                    }
                    return null;
                }));
            }
            for (Future<?> f : futures) {
                f.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
        assertEquals(clientCount * requestsPerClient, okCount.get());
    }

    // ===== Sự cố mạng =====

    @Test
    void clientDisconnectsAbruptly_serverKeepsRunningAndReleasesResources() throws Exception {
        startDefaultServer();

        Socket rude = new Socket("127.0.0.1", server.getPort());
        rude.getOutputStream().write("CHECK_STOCK AT-DE".getBytes(StandardCharsets.UTF_8)); // chưa có xuống dòng
        rude.getOutputStream().flush();
        rude.setSoLinger(true, 0);   // đóng bằng RST, giống mất mạng đột ngột
        rude.close();

        try (TestClient c = new TestClient(server.getPort())) {
            assertTrue(c.request("PING").contains("PONG"));
        }
        waitUntil(() -> server.getActiveClients() == 0, 3000);
        assertEquals(0, server.getActiveClients(), "Kết nối của client đã mất phải được giải phóng");
    }

    @Test
    void idleClient_isDisconnectedAfterTimeout() throws Exception {
        startServer(settings(5, 300), sku -> Optional.empty(), code -> Optional.empty(), "");
        try (TestClient c = new TestClient(server.getPort())) {
            String response = c.readLine();       // không gửi gì, chờ server báo timeout
            assertNotNull(response);
            assertTrue(response.contains("\"code\":\"TIMEOUT\""), response);
            assertNull(c.readLine());
        }
        waitUntil(() -> server.getActiveClients() == 0, 3000);
        assertEquals(0, server.getActiveClients());
    }

    @Test
    void whenServerIsFull_newClientGetsBusyAndSlotIsReusedLater() throws Exception {
        startServer(settings(2, 10000), sku -> Optional.empty(), code -> Optional.empty(), "");
        TestClient c1 = new TestClient(server.getPort());
        TestClient c2 = new TestClient(server.getPort());
        try {
            assertTrue(c1.request("PING").contains("PONG"));
            assertTrue(c2.request("PING").contains("PONG"));

            try (TestClient c3 = new TestClient(server.getPort())) {
                String response = c3.readLine();
                assertNotNull(response);
                assertTrue(response.contains("\"code\":\"BUSY\""), response);
            }

            c1.close();
            waitUntil(() -> server.getActiveClients() == 1, 3000);

            try (TestClient c4 = new TestClient(server.getPort())) {
                assertTrue(c4.request("PING").contains("PONG"));
            }
        } finally {
            c1.close();
            c2.close();
        }
    }

    @Test
    void stop_closesEverything() throws Exception {
        startDefaultServer();
        int port = server.getPort();
        TestClient c = new TestClient(port);
        assertTrue(c.request("PING").contains("PONG"));

        server.stop();

        assertFalse(server.isRunning());
        assertEquals(0, server.getActiveClients());
        assertNull(c.readLine(), "Server đã tắt thì client phải thấy kết nối bị đóng");
        c.close();
        assertThrows(IOException.class, () -> new Socket("127.0.0.1", port).close());
    }
}
