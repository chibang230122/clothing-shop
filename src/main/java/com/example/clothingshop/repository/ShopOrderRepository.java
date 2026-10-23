
package com.example.clothingshop.repository;

import com.example.clothingshop.entity.ShopOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long> {

    List<ShopOrder> findByUser_UserIdOrderByOrderDateDesc(Integer userId);

    java.util.Optional<ShopOrder> findByOrderCode(String orderCode);

    List<ShopOrder> findByOrderStatus(String orderStatus);

    @Query("""
        SELECT COALESCE(SUM(o.totalAmount), 0)
        FROM ShopOrder o
        WHERE o.orderStatus = 'COMPLETED'
          AND o.orderDate >= :startDate
          AND o.orderDate < :endDate
        """)
    BigDecimal calculateRevenue(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
