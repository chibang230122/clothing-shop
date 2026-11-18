package com.example.clothingshop.repository;

import com.example.clothingshop.entity.ShopOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    List<ShopOrder> findByUser_EmailIgnoreCaseOrderByOrderDateDesc(String email);

    java.util.Optional<ShopOrder> findByOrderIdAndUser_EmailIgnoreCase(Long orderId, String email);

    @Query("""
        SELECT o FROM ShopOrder o
        JOIN o.user u
        WHERE (:keyword IS NULL OR :keyword = ''
            OR LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR o.recipientPhone LIKE CONCAT('%', :keyword, '%'))
          AND (:status IS NULL OR :status = '' OR o.orderStatus = :status)
          AND (:fromDate IS NULL OR o.orderDate >= :fromDate)
          AND (:toDate IS NULL OR o.orderDate < :toDate)
        """)
    Page<ShopOrder> searchStaffOrders(
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    @Query("""
        SELECT YEAR(o.orderDate) AS revenueYear,
               MONTH(o.orderDate) AS revenueMonth,
               COALESCE(SUM(o.totalAmount), 0) AS revenueAmount
        FROM ShopOrder o
        WHERE o.orderStatus = 'COMPLETED'
        GROUP BY YEAR(o.orderDate), MONTH(o.orderDate)
        ORDER BY YEAR(o.orderDate) DESC, MONTH(o.orderDate) DESC
        """)
    List<MonthlyRevenueView> findMonthlyRevenueHistory();

    interface MonthlyRevenueView {
        Integer getRevenueYear();
        Integer getRevenueMonth();
        BigDecimal getRevenueAmount();
    }

    long countByOrderStatus(String orderStatus);
}
