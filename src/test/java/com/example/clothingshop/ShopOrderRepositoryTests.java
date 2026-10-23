
package com.example.clothingshop;

import com.example.clothingshop.repository.ShopOrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ShopOrderRepositoryTests {

    @Autowired
    private ShopOrderRepository shopOrderRepository;

    @Test
    void testCalculateRevenue() {
        LocalDateTime startDate =
                LocalDateTime.of(2026, 1, 1, 0, 0);

        LocalDateTime endDate =
                LocalDateTime.of(2027, 1, 1, 0, 0);

        BigDecimal revenue = shopOrderRepository.calculateRevenue(
                startDate,
                endDate
        );

        assertNotNull(revenue);
        assertTrue(revenue.compareTo(BigDecimal.ZERO) >= 0);

        System.out.println("Doanh thu nam 2026: " + revenue);
    }
}
