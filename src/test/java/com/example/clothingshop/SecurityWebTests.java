package com.example.clothingshop;

import com.example.clothingshop.config.SecurityConfig;
import com.example.clothingshop.controller.AuthController;
import com.example.clothingshop.controller.CustomerOrderController;
import com.example.clothingshop.repository.OrderItemRepository;
import com.example.clothingshop.repository.ShopOrderRepository;
import com.example.clothingshop.repository.UserRepository;
import com.example.clothingshop.service.AuthService;
import com.example.clothingshop.service.CustomUserDetailsService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {
        AuthController.class,
        CustomerOrderController.class
})
@Import({
        SecurityConfig.class,
        SecurityWebTests.TestEndpoints.class
})
class SecurityWebTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PasswordEncoder encoder;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private ShopOrderRepository orderRepository;

    @MockitoBean
    private OrderItemRepository orderItemRepository;

    @MockitoBean
    private UserRepository userRepository;

    @RestController
    static class TestEndpoints {

        @GetMapping("/admin/check")
        String admin() {
            return "OK";
        }

        @GetMapping("/staff/check")
        String staff() {
            return "OK";
        }
    }

    @Test
    void correctPasswordCanLogin() throws Exception {

        when(userDetailsService.loadUserByUsername(
                "admin@example.test"))
                .thenReturn(
                        org.springframework.security.core.userdetails.User
                                .withUsername("admin@example.test")
                                .password(encoder.encode("CorrectPass123!"))
                                .roles("ADMIN")
                                .build()
                );

        mvc.perform(post("/login")
                        .with(csrf())
                        .param("email", "admin@example.test")
                        .param("password", "CorrectPass123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void wrongPasswordCannotLogin() throws Exception {

        when(userDetailsService.loadUserByUsername(
                "admin@example.test"))
                .thenReturn(
                        org.springframework.security.core.userdetails.User
                                .withUsername("admin@example.test")
                                .password(encoder.encode("CorrectPass123!"))
                                .roles("ADMIN")
                                .build()
                );

        mvc.perform(post("/login")
                        .with(csrf())
                        .param("email", "admin@example.test")
                        .param("password", "WrongPassword"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void disabledAccountCannotLogin() throws Exception {

        when(userDetailsService.loadUserByUsername(
                "locked@example.test"))
                .thenReturn(
                        org.springframework.security.core.userdetails.User
                                .withUsername("locked@example.test")
                                .password(encoder.encode("CorrectPass123!"))
                                .roles("CUSTOMER")
                                .disabled(true)
                                .build()
                );

        mvc.perform(post("/login")
                        .with(csrf())
                        .param("email", "locked@example.test")
                        .param("password", "CorrectPass123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?locked"));
    }

    @Test
    void customerCannotAccessAdmin() throws Exception {

        mvc.perform(get("/admin/check")
                        .with(user("customer@example.test")
                                .roles("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void staffCannotAccessAdmin() throws Exception {

        mvc.perform(get("/admin/check")
                        .with(user("staff@example.test")
                                .roles("STAFF")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessAdmin() throws Exception {

        mvc.perform(get("/admin/check")
                        .with(user("admin@example.test")
                                .roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void staffCanAccessStaffArea() throws Exception {

        mvc.perform(get("/staff/check")
                        .with(user("staff@example.test")
                                .roles("STAFF")))
                .andExpect(status().isOk());
    }

    @Test
    void guestRedirectedToLogin() throws Exception {

        mvc.perform(get("/admin/check"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void customerCannotViewAnotherOrder() throws Exception {

        mvc.perform(get("/account/orders/77")
                        .with(user("alice@example.test")
                                .roles("CUSTOMER")))
                .andExpect(status().isNotFound());

        verify(orderRepository)
                .findByOrderIdAndUser_EmailIgnoreCase(
                        77L, "alice@example.test"
                );

        verify(orderRepository, never())
                .findById(anyLong());
    }

    @Test
    void postWithoutCsrfIsRejected() throws Exception {

        mvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "admin@example.test")
                        .param("password", "CorrectPass123!"))
                .andExpect(status().isForbidden());
    }
}