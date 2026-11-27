package com.example.clothingshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.context.annotation.ComponentScan(basePackages = {"com.example.clothingshop", "com.clothingshop"})
@org.springframework.boot.autoconfigure.domain.EntityScan(basePackages = {"com.example.clothingshop.entity", "com.clothingshop.chat.entity"})
@org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackages = {"com.example.clothingshop.repository", "com.clothingshop.chat.repository"})
public class ClothingShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClothingShopApplication.class, args);
    }

}
