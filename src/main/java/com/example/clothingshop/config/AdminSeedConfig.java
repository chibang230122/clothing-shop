package com.example.clothingshop.config;

import com.example.clothingshop.entity.Role;
import com.example.clothingshop.entity.User;
import com.example.clothingshop.repository.RoleRepository;
import com.example.clothingshop.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;

@Configuration
@ConditionalOnProperty(
        name = "app.admin.seed.enabled",
        havingValue = "true"
)
public class AdminSeedConfig {

    @Bean
    public CommandLineRunner seedAdmin(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            @Value("${APP_ADMIN_EMAIL:}") String configuredEmail,
            @Value("${APP_ADMIN_PASSWORD:}") String password) {

        return args -> {

            String email = configuredEmail
                    .trim().toLowerCase(Locale.ROOT);

            if (email.isBlank()
                    || password.length() < 12
                    || password.getBytes(
                    StandardCharsets.UTF_8).length > 72) {
                throw new IllegalStateException(
                        "Cấu hình Admin không hợp lệ"
                );
            }

            Role adminRole = roleRepository
                    .findByRoleName("ADMIN")
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "Chưa tồn tại vai trò ADMIN"
                            )
                    );

            User existing = userRepository
                    .findByEmailIgnoreCase(email)
                    .orElse(null);

            if (existing != null) {

                // Tuyệt đối không tự nâng quyền Customer/Staff
                if (!"ADMIN".equals(
                        existing.getRole().getRoleName())) {
                    throw new IllegalStateException(
                            "Email đã thuộc tài khoản không phải Admin"
                    );
                }

                // Chỉ khởi tạo mật khẩu
                if ("REPLACE_WITH_BCRYPT_HASH".equals(
                        existing.getPasswordHash())) {

                    existing.setPasswordHash(
                            passwordEncoder.encode(password)
                    );

                    userRepository.save(existing);
                }

                return;
            }

            User admin = new User();
            admin.setFullName("Quản trị viên");
            admin.setEmail(email);
            admin.setPasswordHash(
                    passwordEncoder.encode(password)
            );
            admin.setRole(adminRole);
            admin.setIsActive(true);
            admin.setCreatedAt(LocalDateTime.now());

            userRepository.save(admin);
        };
    }
}
