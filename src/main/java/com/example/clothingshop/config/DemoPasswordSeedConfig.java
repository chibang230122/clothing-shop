package com.example.clothingshop.config;

import com.example.clothingshop.entity.User;
import com.example.clothingshop.repository.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@ConditionalOnProperty(
        name = "app.demo-password-reset.enabled",
        havingValue = "true"
)
public class DemoPasswordSeedConfig {

    @Bean
    public CommandLineRunner resetDemoPasswords(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            String password = "Shop123456@";

            for (User user : userRepository.findAll()) {

                String role = user.getRole().getRoleName();

                if (("STAFF".equals(role)
                        || "CUSTOMER".equals(role))
                        && "REPLACE_WITH_BCRYPT_HASH"
                        .equals(user.getPasswordHash())) {

                    user.setPasswordHash(
                            passwordEncoder.encode(password)
                    );

                    userRepository.save(user);

                    System.out.println(
                            "Đã khởi tạo mật khẩu: " + user.getEmail()
                    );
                }
            }
        };
    }
}