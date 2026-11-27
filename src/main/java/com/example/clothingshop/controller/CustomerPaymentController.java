package com.example.clothingshop.controller;

import com.example.clothingshop.entity.ShopOrder;
import com.example.clothingshop.repository.ShopOrderRepository;
import com.example.clothingshop.service.OrderPaymentService;
import java.security.Principal;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class CustomerPaymentController {
    private final ShopOrderRepository orderRepository;
    private final OrderPaymentService paymentService;

    public CustomerPaymentController(ShopOrderRepository orderRepository, OrderPaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
    }

    @GetMapping("/account/orders/{id}/payment-status")
    public Map<String, String> status(@PathVariable Long id, Principal principal) {
        ShopOrder order = ownedOrder(id, principal);
        return Map.of("paymentStatus", order.getPaymentStatus());
    }

    @PostMapping("/account/orders/{id}/demo-pay")
    public Map<String, String> demoPay(@PathVariable Long id, Principal principal) {
        paymentService.confirmDemoPayment(id, principal.getName());
        return Map.of("paymentStatus", "PAID", "message", "Mô phỏng thanh toán thành công.");
    }

    private ShopOrder ownedOrder(Long id, Principal principal) {
        return orderRepository.findByOrderIdAndUser_EmailIgnoreCase(id, principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng."));
    }
}
