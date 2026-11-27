package com.clothingshop.chat;

import com.clothingshop.socket.v2.ShopChatSocketServer;
import com.example.clothingshop.ClothingShopApplication;
import com.example.clothingshop.entity.Category;
import com.example.clothingshop.entity.Product;
import com.example.clothingshop.entity.ProductVariant;
import com.example.clothingshop.entity.Role;
import com.example.clothingshop.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
    classes = ClothingShopApplication.class,
    properties = {
        "shop.socket.enabled=false",
        "shop.chat-socket.enabled=true",
        "shop.chat-socket.port=0"
    }
)
class ChatAndLockingSocketV2Tests {

    @Autowired
    private ShopChatSocketServer chatSocketServer;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TransactionTemplate txTemplate;

    @PersistenceContext
    private EntityManager em;

    private String suffix;
    private String testEmail;
    private String testSku;
    private Integer createdUserId;
    private Integer createdVariantId;
    private Integer createdProductId;
    private Integer createdCategoryId;

    @BeforeEach
    void seedTestData() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        testEmail = "v2test_" + suffix + "@clothingshop.local";
        testSku = "V2SKU-" + suffix.toUpperCase();

        txTemplate.executeWithoutResult(status -> {
            List<Role> roles = em.createQuery("SELECT r FROM Role r WHERE UPPER(r.roleName) = 'ADMIN'", Role.class)
                .setMaxResults(1)
                .getResultList();
            Role adminRole;
            if (roles.isEmpty()) {
                adminRole = new Role();
                adminRole.setRoleName("ADMIN");
                em.persist(adminRole);
            } else {
                adminRole = roles.get(0);
            }

            User user = new User();
            user.setRole(adminRole);
            user.setFullName("V2 Test Admin " + suffix);
            user.setEmail(testEmail);
            user.setPasswordHash(passwordEncoder.encode("Admin@123"));
            user.setIsActive(true);
            user.setCreatedAt(LocalDateTime.now());
            em.persist(user);
            createdUserId = user.getUserId();

            Category cat = new Category();
            cat.setCategoryName("V2 Cat " + suffix);
            cat.setIsActive(true);
            em.persist(cat);
            createdCategoryId = cat.getCategoryId();

            Product prod = new Product();
            prod.setCategory(cat);
            prod.setProductName("V2 Test Shirt " + suffix);
            prod.setBasePrice(new BigDecimal("150000.00"));
            prod.setIsActive(true);
            prod.setCreatedAt(LocalDateTime.now());
            em.persist(prod);
            createdProductId = prod.getProductId();

            ProductVariant variant = new ProductVariant();
            variant.setProduct(prod);
            variant.setSku(testSku);
            variant.setSize("L");
            variant.setColor("Black");
            variant.setStockQuantity(10);
            variant.setIsActive(true);
            em.persist(variant);
            createdVariantId = variant.getVariantId();
        });
    }

    @AfterEach
    void cleanupTestData() {
        txTemplate.executeWithoutResult(status -> {
            if (createdVariantId != null) {
                ProductVariant v = em.find(ProductVariant.class, createdVariantId);
                if (v != null) em.remove(v);
            }
            if (createdProductId != null) {
                Product p = em.find(Product.class, createdProductId);
                if (p != null) em.remove(p);
            }
            if (createdCategoryId != null) {
                Category c = em.find(Category.class, createdCategoryId);
                if (c != null) em.remove(c);
            }
            if (createdUserId != null) {
                User u = em.find(User.class, createdUserId);
                if (u != null) em.remove(u);
            }
        });
    }

    @Test
    void shouldAuthenticateAndHandlePingOnDedicatedChatPort() throws Exception {
        int port = chatSocketServer.getBoundPort();
        try (Socket s = new Socket("127.0.0.1", port);
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8))) {

            String welcome = in.readLine();
            assertNotNull(welcome);
            assertTrue(welcome.contains("\"WELCOME\""));

            out.write("{\"v\":2,\"type\":\"PING\",\"requestId\":\"t1\"}\n");
            out.flush();
            String pong = in.readLine();
            assertNotNull(pong);
            assertTrue(pong.contains("\"PONG\""));
        }
    }

    @Test
    void shouldSerializeConcurrentStockUpdatesWithoutLostUpdates() throws Exception {
        int port = chatSocketServer.getBoundPort();
        int workers = 6;
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < workers; i++) {
            final int idx = i;
            futures.add(pool.submit(() -> {
                try (Socket s = new Socket("127.0.0.1", port);
                     BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
                     BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8))) {

                    in.readLine();
                    out.write("{\"v\":2,\"type\":\"AUTH\",\"requestId\":\"a" + idx + "\",\"payload\":{\"username\":\"" + testEmail + "\",\"password\":\"Admin@123\"}}\n");
                    out.flush();
                    String authResp = in.readLine();
                    assertNotNull(authResp);
                    assertTrue(authResp.contains("\"AUTH_OK\""));

                    startGate.await();
                    out.write("{\"v\":2,\"type\":\"STOCK_ADJUST\",\"requestId\":\"u" + idx + "\",\"payload\":{\"sku\":\"" + testSku + "\",\"delta\":1,\"reason\":\"Test concurrency\"}}\n");
                    out.flush();
                    String expectedReqTag = "\"requestId\":\"u" + idx + "\"";
                    String line;
                    while ((line = in.readLine()) != null) {
                        if (line.contains(expectedReqTag)) {
                            return line;
                        }
                    }
                    return null;
                }
            }));
        }

        startGate.countDown();
        for (Future<String> f : futures) {
            String resp = f.get(10, TimeUnit.SECONDS);
            assertNotNull(resp);
            assertTrue(resp.contains("\"STOCK_UPDATE_OK\""), "Unexpected response: " + resp);
        }
        pool.shutdownNow();

        Integer finalStock = em.createQuery("SELECT v.stockQuantity FROM ProductVariant v WHERE v.variantId = :id", Integer.class)
            .setParameter("id", createdVariantId)
            .getSingleResult();
        assertEquals(16, finalStock);
    }
}