package com.example.clothingshop;

import com.example.clothingshop.dto.RegisterForm;
import com.example.clothingshop.entity.Role;
import com.example.clothingshop.entity.User;
import com.example.clothingshop.repository.RoleRepository;
import com.example.clothingshop.repository.UserRepository;
import com.example.clothingshop.service.AuthService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private RegisterForm validForm() {
        RegisterForm form = new RegisterForm();
        form.setFullName("Nguyen Van A");
        form.setEmail("customer@example.test");
        form.setPhone("0901234567");
        form.setAddress("TP. Ho Chi Minh");
        form.setPassword("CorrectPass123!");
        form.setConfirmPassword("CorrectPass123!");
        return form;
    }

    @Test
    void registerAlwaysCreatesCustomer() {

        Role customer = new Role();
        customer.setRoleName("CUSTOMER");

        when(userRepository.existsByEmailIgnoreCase(
                "customer@example.test")).thenReturn(false);

        when(roleRepository.findByRoleName("CUSTOMER"))
                .thenReturn(Optional.of(customer));

        when(passwordEncoder.encode(anyString()))
                .thenReturn("BCryptHashedPassword");

        authService.registerCustomer(validForm());

        ArgumentCaptor<User> captor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).saveAndFlush(
                captor.capture()
        );

        User saved = captor.getValue();

        assertEquals("CUSTOMER",
                saved.getRole().getRoleName());

        assertEquals("BCryptHashedPassword",
                saved.getPasswordHash());

        assertTrue(saved.getIsActive());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void duplicateEmailIsRejected() {

        when(userRepository.existsByEmailIgnoreCase(
                "customer@example.test")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> authService.registerCustomer(validForm()));

        verify(userRepository, never())
                .saveAndFlush(any(User.class));
    }
}
