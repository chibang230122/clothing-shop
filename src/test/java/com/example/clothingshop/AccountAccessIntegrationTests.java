package com.example.clothingshop;

import com.example.clothingshop.entity.Category;
import com.example.clothingshop.entity.Product;
import com.example.clothingshop.entity.ProductVariant;
import com.example.clothingshop.entity.Role;
import com.example.clothingshop.repository.CategoryRepository;
import com.example.clothingshop.repository.ProductRepository;
import com.example.clothingshop.repository.ProductVariantRepository;
import com.example.clothingshop.repository.ShopOrderRepository;
import com.example.clothingshop.entity.User;
import com.example.clothingshop.repository.RoleRepository;
import com.example.clothingshop.repository.UserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:account-access;MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "app.admin.seed.enabled=false",
        "shop.socket.enabled=false",
        "spring.mail.host=smtp.example.test",
        "spring.mail.properties.mail.smtp.auth=false",
        "app.password-reset.from=shop@example.test"
})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountAccessIntegrationTests {

    private static final String PASSWORD = "FixturePass123!";
    private static final Set<String> PUBLIC_ROUTES = Set.of(
            "/", "/products", "/products/{id}", "/login", "/register",
            "/forgot-password", "/reset-password", "/access-denied"
    );

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private RoleRepository roles;
    @Autowired private CategoryRepository categories;
    @Autowired private ProductRepository products;
    @Autowired private ProductVariantRepository variants;
    @Autowired private ShopOrderRepository orders;
    @Autowired private PasswordEncoder encoder;
    @Autowired @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mappings;
    @MockitoBean private JavaMailSender mailSender;

    @BeforeAll
    void seedAccounts() {
        for (String name : Set.of("ADMIN", "STAFF", "CUSTOMER")) {
            Role role = new Role();
            role.setRoleName(name);
            roles.save(role);
            addAccount(email(name), name + " Test", role);
        }
        Role customer = roles.findByRoleName("CUSTOMER").orElseThrow();
        addAccount("reset@example.test", "Reset Test", customer);
        addAccount("profile@example.test", "Profile Test", customer);
        addAccount("promoted@example.test", "Promoted Test", customer);
        addAccount("buyer@example.test", "Buyer Test", customer);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "STAFF", "CUSTOMER"})
    void allApplicationControllerRoutesRespectTheRole(String role) throws Exception {
        MockHttpSession session = signIn(email(role), PASSWORD);
        for (var entry : mappings.getHandlerMethods().entrySet()) {
            if (!entry.getValue().getBeanType().getPackageName()
                    .equals("com.example.clothingshop.controller")) {
                continue;
            }
            for (String route : entry.getKey().getPatternValues()) {
                for (RequestMethod method : entry.getKey().getMethodsCondition().getMethods()) {
                    boolean allowed = isAllowed(route, role);
                    String url = route.replaceAll("\\{[^}]+}", "999999");
                    var request = request(method.asHttpMethod(), url).session(session).with(csrf());
                    if (route.equals("/admin/products/update")) {
                        request.param("productId", "999999");
                    }
                    if (route.equals("/admin/categories/update")) {
                        request.param("categoryId", "999999");
                    }
                    MvcResult result = mvc.perform(request).andReturn();
                    if (allowed) {
                        assertNotNull(result.getHandler(), method + " " + route + " " + role);
                    } else {
                        assertEquals(403, result.getResponse().getStatus(), method + " " + route + " " + role);
                        assertNull(result.getHandler(), method + " " + route + " " + role);
                    }
                }
            }
        }
        mvc.perform(get("/css/admin.css").session(session)).andExpect(status().isOk());
        mvc.perform(get("/js/password-visibility.js").session(session)).andExpect(status().isOk());
        mvc.perform(get("/not-a-registered-route").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(role + " Test · " + role)));
        mvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    void anOpenCustomerSessionLosesCartAccessAfterPromotion() throws Exception {
        MockHttpSession session = signIn("promoted@example.test", PASSWORD);
        User user = users.findByEmailIgnoreCase("promoted@example.test").orElseThrow();
        user.setRole(roles.findByRoleName("STAFF").orElseThrow());
        users.save(user);

        mvc.perform(get("/cart").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/checkout").session(session).with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    void onlyAdminCanEditCustomerInformation() throws Exception {
        User customer = users.findByEmailIgnoreCase("profile@example.test").orElseThrow();
        String url = "/admin/accounts/" + customer.getUserId() + "/edit";
        var admin = signIn(email("ADMIN"), PASSWORD);
        mvc.perform(get("/admin/accounts").session(admin).param("keyword", customer.getEmail()))
                .andExpect(content().string(containsString(url)));
        mvc.perform(get(url).session(admin)).andExpect(status().isOk());
        for (String role : Set.of("STAFF", "CUSTOMER")) {
            var session = signIn(email(role), PASSWORD);
            mvc.perform(get(url).session(session)).andExpect(status().isForbidden());
            mvc.perform(post(url).session(session).with(csrf()).param("fullName", "Denied"))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(post(url).session(admin).with(csrf()).param("fullName", "  "))
                .andExpect(model().attributeHasFieldErrors("form", "fullName"));
        mvc.perform(post(url).session(admin).with(csrf()).param("fullName", "Admin Name")
                        .param("phone", "0905551234").param("address", "Trà Vinh")
                        .param("email", "ignored@example.test").param("isActive", "false"))
                .andExpect(redirectedUrl("/admin/accounts"));
        User updated = users.findById(customer.getUserId()).orElseThrow();
        assertEquals("Admin Name", updated.getFullName());
        assertEquals("0905551234", updated.getPhone());
        assertEquals("Trà Vinh", updated.getAddress());
        assertEquals(customer.getEmail(), updated.getEmail());
        assertTrue(updated.getIsActive());
        User staff = users.findByEmailIgnoreCase(email("STAFF")).orElseThrow();
        mvc.perform(post("/admin/accounts/" + staff.getUserId() + "/edit")
                        .session(admin).with(csrf()).param("fullName", "Updated Staff"))
                .andExpect(redirectedUrl("/admin/accounts"));
        assertEquals("Updated Staff", users.findById(staff.getUserId()).orElseThrow().getFullName());
        staff.setFullName("STAFF Test");
        users.save(staff);
    }

    @Test
    void adminCanCreateStaffWhoCanSignIn() throws Exception {
        var admin = signIn(email("ADMIN"), PASSWORD);
        String path = "/admin/accounts/staff/new";
        mvc.perform(get(path).session(admin)).andExpect(status().isOk());
        mvc.perform(post(path).session(admin).with(csrf()).param("fullName", "New Staff")
                        .param("email", "newstaff@example.test").param("password", PASSWORD)
                        .param("confirmPassword", PASSWORD).param("roleName", "ADMIN"))
                .andExpect(redirectedUrl("/admin/accounts"));
        var staff = users.findByEmailIgnoreCase("newstaff@example.test").orElseThrow();
        assertEquals("STAFF", staff.getRole().getRoleName());
        mvc.perform(get("/staff/orders").session(signIn(staff.getEmail(), PASSWORD))).andExpect(status().isOk());
    }

    @Test
    void customerCanEditOwnProfileAndSeeUpdatedName() throws Exception {
        MockHttpSession session = signIn("profile@example.test", PASSWORD);
        mvc.perform(post("/account/profile").session(session).with(csrf())
                        .param("fullName", "Tên mới").param("phone", "0901234567")
                        .param("address", "Trà Vinh").param("email", "another@example.test")
                        .param("role.roleName", "ADMIN"))
                .andExpect(redirectedUrl("/account/profile?success"));
        User user = users.findByEmailIgnoreCase("profile@example.test").orElseThrow();
        assertEquals("Tên mới", user.getFullName());
        assertEquals("0901234567", user.getPhone());
        assertEquals("Trà Vinh", user.getAddress());
        assertEquals("CUSTOMER", user.getRole().getRoleName());
        mvc.perform(get("/account/profile").session(session))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Tên mới · CUSTOMER")));
    }

    @Test
    void customerPlacesOrderAndStaffCannotSubmitTheSameCheckoutRequest() throws Exception {
        Category category = new Category();
        category.setCategoryName("Kiểm thử đặt hàng");
        categories.save(category);

        Product product = new Product();
        product.setCategory(category);
        product.setProductName("Áo kiểm thử");
        product.setBasePrice(BigDecimal.valueOf(100_000));
        product.setCreatedAt(LocalDateTime.now());
        products.save(product);

        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSku("TEST-CHECKOUT");
        variant.setSize("M");
        variant.setColor("Đen");
        variant.setStockQuantity(5);
        variants.save(variant);

        for (String role : Set.of("ADMIN", "STAFF")) {
            MockHttpSession session = signIn(email(role), PASSWORD);
            mvc.perform(get("/products/" + product.getProductId()).session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("/cart/add"))));
            mvc.perform(checkoutRequest(session)).andExpect(status().isForbidden());
        }
        assertEquals(0, orders.count());

        MockHttpSession buyer = signIn("buyer@example.test", PASSWORD);
        mvc.perform(get("/products/" + product.getProductId()).session(buyer))
                .andExpect(status().isOk()).andExpect(content().string(containsString("/cart/add")));
        mvc.perform(post("/cart/add").session(buyer).with(csrf())
                        .param("variantId", variant.getVariantId().toString()))
                .andExpect(redirectedUrl("/cart"));
        mvc.perform(get("/checkout").session(buyer)).andExpect(status().isOk());
        mvc.perform(checkoutRequest(buyer)).andExpect(redirectedUrlPattern("/account/orders/*"));

        var order = orders.findAll().get(0);
        assertEquals("buyer@example.test", order.getUser().getEmail());
        MockHttpSession staff = signIn(email("STAFF"), PASSWORD);
        String invoiceUrl = "/api/invoices/" + order.getOrderId();
        byte[] document = mvc.perform(get(invoiceUrl).session(staff))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Áo kiểm thử")))
                .andExpect(content().string(containsString(order.getOrderCode())))
                .andReturn().getResponse().getContentAsByteArray();
        mvc.perform(post(invoiceUrl + "/email").session(staff)).andExpect(status().isForbidden());
        var message = new jakarta.mail.internet.MimeMessage((jakarta.mail.Session) null);
        org.mockito.Mockito.when(mailSender.createMimeMessage()).thenReturn(message);
        mvc.perform(post(invoiceUrl + "/email").session(staff).with(csrf())
                .param("email", "another@example.test")).andExpect(status().isOk());
        verify(mailSender).send(message);
        assertEquals("buyer@example.test", message.getAllRecipients()[0].toString());
        message.saveChanges();
        var multipart = (jakarta.mail.Multipart) message.getContent();
        var file = multipart.getBodyPart(multipart.getCount() - 1);
        assertEquals("hoa-don-" + order.getOrderId() + ".html", file.getFileName());
        assertArrayEquals(document, file.getInputStream().readAllBytes());
        assertEquals(4, variants.findById(variant.getVariantId()).orElseThrow().getStockQuantity());
        mvc.perform(get("/account/orders/" + order.getOrderId()).session(buyer)).andExpect(status().isOk());
        mvc.perform(get("/account/orders/" + order.getOrderId()).session(signIn(email("CUSTOMER"), PASSWORD)))
                .andExpect(status().isNotFound());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder checkoutRequest(
            MockHttpSession session) {
        return post("/checkout").session(session).with(csrf())
                .param("recipientName", "Người nhận kiểm thử")
                .param("recipientPhone", "0901234567").param("shippingAddress", "Trà Vinh")
                .param("paymentMethod", "COD");
    }

    @Test
    void googleOptionsRemainVisibleAndPasswordErrorAppearsAfterSubmit() throws Exception {
        mvc.perform(get("/login")).andExpect(content().string(containsString("Đăng nhập bằng Google")));
        var page = mvc.perform(get("/register")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Đăng ký bằng Google")))
                .andReturn().getResponse();
        String guide = com.example.clothingshop.service.PasswordPolicy.GUIDE;
        assertFalse(page.getContentAsString(java.nio.charset.StandardCharsets.UTF_8)
                .replaceAll("<[^>]+>", "").contains(guide));
        for (String password : java.util.List.of("", "Ab1!", "LowercaseOnly")) {
            mvc.perform(post("/register").with(csrf()).param("fullName", "Test")
                            .param("email", "test@example.test").param("password", password)
                            .param("confirmPassword", password))
                    .andExpect(model().attributeHasFieldErrors("form", "password"))
                    .andExpect(content().string(containsString(">" + guide + "</small>")));
        }
    }

    @Test
    void allPasswordFormsRejectAWeakNewPassword() throws Exception {
        String weak = "alllowercase";
        mvc.perform(post("/register").with(csrf()).param("email", "weak@example.test")
                        .param("fullName", "Khách thử").param("password", weak).param("confirmPassword", weak))
                .andExpect(model().attributeHasFieldErrors("form", "password"));
        assertFalse(users.existsByEmailIgnoreCase("weak@example.test"));
        mvc.perform(post("/account/change-password").with(csrf()).session(signIn(email("STAFF"), PASSWORD))
                        .param("currentPassword", PASSWORD).param("newPassword", weak).param("confirmPassword", weak))
                .andExpect(model().attributeHasFieldErrors("form", "newPassword"));
        MockHttpSession pending = new MockHttpSession();
        pending.setAttribute("com.example.clothingshop.controller.PasswordResetController.email", "reset@example.test");
        mvc.perform(post("/reset-password").with(csrf()).session(pending).param("otp", "000000")
                        .param("newPassword", weak).param("confirmPassword", weak))
                .andExpect(model().attribute("error", com.example.clothingshop.service.PasswordPolicy.GUIDE));
    }

    @Test
    void wrongLoginOffersOtpAndResetRequiresTheOriginalSession() throws Exception {
        MockHttpSession oldLogin = signIn("reset@example.test", PASSWORD);
        mvc.perform(post("/login").with(csrf()).param("email", "reset@example.test")
                        .param("password", "wrong"))
                .andExpect(redirectedUrl("/login?error"));
        mvc.perform(get("/login?error")).andExpect(status().isOk())
                .andExpect(content().string(containsString("/forgot-password")))
                .andExpect(content().string(containsString("aria-controls=\"password\"")));

        mvc.perform(get("/forgot-password")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Gửi mã OTP")));
        mvc.perform(post("/forgot-password").param("email", "reset@example.test"))
                .andExpect(status().isForbidden());

        MockHttpSession reset = requestCode("reset@example.test");
        MockHttpSession another = requestCode("reset@example.test");
        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, timeout(5000).times(1)).send(sent.capture());
        assertArrayEquals(new String[]{"reset@example.test"}, sent.getValue().getTo());
        var match = Pattern.compile("\\b([0-9]{6})\\b").matcher(sent.getValue().getText());
        assertTrue(match.find());
        String code = match.group(1);
        String wrongCode = code.equals("000000") ? "000001" : "000000";

        mvc.perform(get("/reset-password").session(reset)).andExpect(status().isOk());
        mvc.perform(resetRequest(another, code)).andExpect(view().name("auth/reset-password"));
        mvc.perform(resetRequest(reset, wrongCode)).andExpect(view().name("auth/reset-password"));
        mvc.perform(resetRequest(reset, code)).andExpect(redirectedUrl("/login?reset"));
        mvc.perform(resetRequest(another, code)).andExpect(view().name("auth/reset-password"));
        mvc.perform(get("/account/profile").session(oldLogin)).andExpect(redirectedUrl("/login?expired"));
        mvc.perform(post("/login").with(csrf()).param("email", "reset@example.test").param("password", PASSWORD))
                .andExpect(redirectedUrl("/login?error"));
        signIn("reset@example.test", "NewPassword456!");
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder resetRequest(
            MockHttpSession session, String code) {
        return post("/reset-password").session(session).with(csrf()).param("otp", code)
                .param("newPassword", "NewPassword456!").param("confirmPassword", "NewPassword456!");
    }

    private MockHttpSession requestCode(String email) throws Exception {
        return (MockHttpSession) mvc.perform(post("/forgot-password").with(csrf()).param("email", email))
                .andExpect(redirectedUrl("/reset-password")).andReturn().getRequest().getSession(false);
    }

    private MockHttpSession signIn(String email, String password) throws Exception {
        return (MockHttpSession) mvc.perform(post("/login").with(csrf())
                        .param("email", email).param("password", password))
                .andExpect(authenticated().withUsername(email)).andReturn().getRequest().getSession(false);
    }

    private void addAccount(String email, String fullName, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole(role);
        user.setIsActive(true);
        user.setPasswordHash(encoder.encode(PASSWORD));
        user.setCreatedAt(LocalDateTime.now());
        users.save(user);
    }

    private static boolean isAllowed(String route, String role) {
        if (route.startsWith("/admin")) {
            return role.equals("ADMIN");
        }
        if (route.startsWith("/staff") || route.startsWith("/api/invoices")) {
            return role.equals("ADMIN") || role.equals("STAFF");
        }
        if (route.startsWith("/cart") || route.startsWith("/checkout") || route.startsWith("/account/orders")) {
            return role.equals("CUSTOMER");
        }
        return route.startsWith("/account/") || PUBLIC_ROUTES.contains(route);
    }

    private static String email(String role) {
        return role.toLowerCase(java.util.Locale.ROOT) + "@example.test";
    }
}
