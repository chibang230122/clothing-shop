package com.example.clothingshop.service;

import com.example.clothingshop.dto.ProfileForm;
import com.example.clothingshop.entity.User;
import com.example.clothingshop.repository.UserRepository;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public User getCurrentUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Không tìm thấy tài khoản"
                        )
                );
    }

    @Transactional
    public void updateProfile(
            String email, ProfileForm form) {

        User user = getCurrentUser(email);

        user.setFullName(form.getFullName().trim());

        user.setPhone(
                form.getPhone() == null
                        ? null : form.getPhone().trim()
        );

        user.setAddress(
                form.getAddress() == null
                        ? null : form.getAddress().trim()
        );

        // Không sửa Role, Email, PasswordHash, IsActive
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(
            String email,
            String currentPassword,
            String newPassword) {

        User user = getCurrentUser(email);

        if (!passwordEncoder.matches(
                currentPassword, user.getPasswordHash())) {

            throw new IllegalArgumentException(
                    "Mật khẩu hiện tại không chính xác"
            );
        }

        if (newPassword.getBytes(
                StandardCharsets.UTF_8).length > 72) {

            throw new IllegalArgumentException(
                    "Mật khẩu mới vượt quá giới hạn BCrypt"
            );
        }

        user.setPasswordHash(
                passwordEncoder.encode(newPassword)
        );

        userRepository.save(user);
    }
}
