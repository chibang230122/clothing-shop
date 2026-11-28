package com.example.clothingshop.service;

import com.example.clothingshop.entity.Voucher;
import com.example.clothingshop.repository.VoucherRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class VoucherService {

    private final VoucherRepository voucherRepository;

    public VoucherService(VoucherRepository voucherRepository) {
        this.voucherRepository = voucherRepository;
    }

    public record VoucherResult(
            boolean valid,
            String code,
            BigDecimal discountAmount,
            BigDecimal finalTotal,
            String message
    ) {}

    public VoucherResult evaluate(String rawCode, BigDecimal subTotal) {
        BigDecimal safeSubTotal = subTotal != null ? subTotal : BigDecimal.ZERO;
        if (rawCode == null || rawCode.trim().isEmpty()) {
            return null;
        }

        String cleanCode = rawCode.trim().toUpperCase();
        Voucher v = voucherRepository.findByCodeIgnoreCase(cleanCode).orElse(null);
        LocalDateTime now = LocalDateTime.now();

        if (v == null || Boolean.FALSE.equals(v.getIsActive())) {
            return new VoucherResult(false, cleanCode, BigDecimal.ZERO, safeSubTotal,
                    "Mã giảm giá không tồn tại hoặc đã ngừng áp dụng.");
        }
        if (v.getStartDate() != null && now.isBefore(v.getStartDate())) {
            return new VoucherResult(false, cleanCode, BigDecimal.ZERO, safeSubTotal,
                    "Mã giảm giá chưa đến thời gian sử dụng.");
        }
        if (v.getEndDate() != null && now.isAfter(v.getEndDate())) {
            return new VoucherResult(false, cleanCode, BigDecimal.ZERO, safeSubTotal,
                    "Mã giảm giá đã hết hạn.");
        }
        if (v.getQuantity() == null || v.getQuantity() <= 0) {
            return new VoucherResult(false, cleanCode, BigDecimal.ZERO, safeSubTotal,
                    "Mã giảm giá đã hết lượt sử dụng.");
        }
        if (v.getMinOrderAmount() != null && safeSubTotal.compareTo(v.getMinOrderAmount()) < 0) {
            return new VoucherResult(false, cleanCode, BigDecimal.ZERO, safeSubTotal,
                    "Đơn hàng chưa đạt giá trị tối thiểu để dùng mã này.");
        }

        BigDecimal discount;
        if ("PERCENT".equalsIgnoreCase(v.getDiscountType())) {
            discount = safeSubTotal.multiply(v.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            if (v.getMaxDiscountAmount() != null && discount.compareTo(v.getMaxDiscountAmount()) > 0) {
                discount = v.getMaxDiscountAmount();
            }
        } else {
            discount = v.getDiscountValue();
        }

        if (discount.compareTo(safeSubTotal) > 0) {
            discount = safeSubTotal;
        }
        BigDecimal finalTotal = safeSubTotal.subtract(discount);

        return new VoucherResult(true, v.getCode(), discount, finalTotal,
                "Đã áp dụng mã " + v.getCode() + " thành công!");
    }
}