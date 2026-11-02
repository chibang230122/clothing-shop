package com.example.clothingshop.service;

import com.example.clothingshop.dto.RegisterForm;
import com.example.clothingshop.entity.Role;
import com.example.clothingshop.entity.User;
import com.example.clothingshop.repository.RoleRepository;
import com.example.clothingshop.repository.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void registerCustomer(RegisterForm form) {

        String email = form.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException(
                    "Email này đã được đăng ký"
            );
        }

        if (form.getPassword()
                .getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException(
                    "Mật khẩu vượt quá giới hạn của BCrypt"
            );
        }

        Role customerRole = roleRepository
                .findByRoleName("CUSTOMER")
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Chưa cấu hình vai trò CUSTOMER"
                        )
                );

        User user = new User();

        user.setFullName(form.getFullName().trim());
        user.setEmail(email);
        user.setPhone(
                form.getPhone() == null
                        ? null : form.getPhone().trim()
        );
        user.setAddress(
                form.getAddress() == null
                        ? null : form.getAddress().trim()
        );

        user.setPasswordHash(
                passwordEncoder.encode(form.getPassword())
        );

        // Cố định quyền khi đăng ký
        user.setRole(customerRole);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException(
                    "Không thể đăng ký, email có thể đã tồn tại",
                    ex
            );
        }
    }
}
