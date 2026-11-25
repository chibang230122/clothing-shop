package com.example.clothingshop.service;

import com.example.clothingshop.dto.ProfileForm;
import com.example.clothingshop.entity.User;
import com.example.clothingshop.repository.UserRepository;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


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

        applyProfile(user, form);
    }

    static void applyProfile(User user, ProfileForm form) {
        user.setFullName(form.getFullName().trim());
        user.setPhone(form.getPhone() == null ? null : form.getPhone().trim());
        user.setAddress(form.getAddress() == null ? null : form.getAddress().trim());
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

        PasswordPolicy.require(newPassword);

        user.setPasswordHash(
                passwordEncoder.encode(newPassword)
        );

        userRepository.save(user);
    }
}
