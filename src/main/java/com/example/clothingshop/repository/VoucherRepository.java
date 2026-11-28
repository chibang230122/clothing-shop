package com.example.clothingshop.repository;

import com.example.clothingshop.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Integer> {

    boolean existsByCodeIgnoreCase(String code);

    Optional<Voucher> findByCodeIgnoreCase(String code);

    @Query("""
        SELECT v FROM Voucher v
        WHERE v.isActive = true
          AND v.quantity > 0
          AND v.startDate <= :now
          AND v.endDate >= :now
        ORDER BY v.minOrderAmount ASC
    """)
    List<Voucher> findAvailableVouchers(@Param("now") LocalDateTime now);
}