package com.example.clothingshop.service;

import com.example.clothingshop.entity.ShopOrder;
import com.example.clothingshop.repository.ShopOrderRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderPaymentService {

    @PersistenceContext
    private EntityManager em;

    private final ShopOrderRepository orderRepository;

    @Value("${app.payment.mode:DEMO}")
    private String paymentMode;

    public OrderPaymentService(ShopOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /** Staff manual confirmation retained for the existing staff workflow. */
    @Transactional
    public void confirmPayment(Long id) {
        ShopOrder order = em.find(ShopOrder.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (order == null) throw new IllegalArgumentException("Không tìm thấy đơn hàng.");
        if (!"UNPAID".equals(order.getPaymentStatus())) throw new IllegalStateException("Đơn hàng không ở trạng thái chờ thanh toán.");
        if (!"BANK_TRANSFER".equals(order.getPaymentMethod())) throw new IllegalStateException("Chỉ xác nhận chuyển khoản cho đơn thanh toán QR.");
        order.setPaymentStatus("PAID");
    }

    /** Demo-only action. Never enabled when running with SePay webhook mode. */
    @Transactional
    public void confirmDemoPayment(Long orderId, String customerEmail) {
        if (!"DEMO".equalsIgnoreCase(paymentMode)) {
            throw new IllegalStateException("Nút mô phỏng chỉ hoạt động trong chế độ DEMO.");
        }
        ShopOrder order = orderRepository.findByOrderIdAndUser_EmailIgnoreCase(orderId, customerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
        order = em.find(ShopOrder.class, order.getOrderId(), LockModeType.PESSIMISTIC_WRITE);
        if (!"BANK_TRANSFER".equals(order.getPaymentMethod())) throw new IllegalStateException("Đơn này không thanh toán bằng QR ngân hàng.");
        if (!"UNPAID".equals(order.getPaymentStatus())) throw new IllegalStateException("Đơn hàng đã được xử lý thanh toán.");
        order.setPaymentStatus("PAID");
    }

    /** Handle a SePay incoming transaction. Repeated webhook deliveries are idempotent. */
    @Transactional
    public boolean processSePayIncoming(String paymentCode, String content, String transferType, BigDecimal amount) {
        if (!"in".equalsIgnoreCase(transferType) || amount == null || amount.signum() <= 0) return false;
        String orderCode = paymentCode;
        if (orderCode == null || orderCode.isBlank()) orderCode = content;
        if (orderCode == null || orderCode.isBlank()) return false;

        ShopOrder matched = null;
        // Prefer SePay's extracted `code`; fallback to the exact order code embedded in content.
        if (paymentCode != null && !paymentCode.isBlank()) {
            matched = orderRepository.findByOrderCode(paymentCode.trim()).orElse(null);
        }
        if (matched == null && content != null) {
            String normalized = content.toUpperCase(Locale.ROOT);
            // Order codes are generated as DH + 20 hexadecimal characters.
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("DH[0-9A-F]{20}").matcher(normalized);
            if (matcher.find()) matched = orderRepository.findByOrderCode(matcher.group()).orElse(null);
        }
        if (matched == null) return false;

        ShopOrder order = em.find(ShopOrder.class, matched.getOrderId(), LockModeType.PESSIMISTIC_WRITE);
        if (order == null || !"BANK_TRANSFER".equals(order.getPaymentMethod())) return false;
        if (!"UNPAID".equals(order.getPaymentStatus())) return true; // safe on webhook retry/replay
        if (amount.compareTo(order.getTotalAmount()) < 0) return false;
        order.setPaymentStatus("PAID");
        return true;
    }
}
