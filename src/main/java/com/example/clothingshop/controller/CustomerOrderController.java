
package com.example.clothingshop.controller;

import com.example.clothingshop.entity.ShopOrder;
import com.example.clothingshop.repository.OrderItemRepository;
import com.example.clothingshop.repository.ShopOrderRepository;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/account/orders")
public class CustomerOrderController {

    private final ShopOrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    @Value("${app.payment.bank-code:}") private String bankCode;
    @Value("${app.payment.account-number:}") private String accountNumber;
    @Value("${app.payment.account-name:}") private String accountName;
    @Value("${app.payment.mode:DEMO}") private String paymentMode;

    public CustomerOrderController(
            ShopOrderRepository orderRepository,
            OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @GetMapping
    public String myOrders(
            Principal principal,
            Model model) {

        model.addAttribute(
                "orders",
                orderRepository.findByUser_EmailIgnoreCaseOrderByOrderDateDesc(
                        principal.getName()
                )
        );

        return "account/orders";
    }

    @GetMapping("/{id}")
    public String orderDetail(
            @PathVariable Long id,
            Principal principal,
            Model model) {

        ShopOrder order = orderRepository
                .findByOrderIdAndUser_EmailIgnoreCase(
                        id,
                        principal.getName()
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy đơn hàng"
                        )
                );

        model.addAttribute("order", order);
        model.addAttribute("paymentMode", paymentMode);
        model.addAttribute("bankCode", bankCode);
        model.addAttribute("paymentQrConfigured", bankCode != null && !bankCode.isBlank() && accountNumber != null && !accountNumber.isBlank());

        if ("BANK_TRANSFER".equals(order.getPaymentMethod()) && bankCode != null && !bankCode.isBlank() && accountNumber != null && !accountNumber.isBlank()) {
            String info = URLEncoder.encode("THANH TOAN " + order.getOrderCode(), StandardCharsets.UTF_8);
            String name = URLEncoder.encode(accountName == null ? "" : accountName, StandardCharsets.UTF_8);
            model.addAttribute("paymentQrUrl", "https://vietqr.app/img?acc=" + URLEncoder.encode(accountNumber, StandardCharsets.UTF_8) + "&bank=" + URLEncoder.encode(bankCode, StandardCharsets.UTF_8) + "&amount=" + order.getTotalAmount().longValue() + "&des=" + info);
        }
        model.addAttribute(
                "orderItems",
                orderItemRepository.findByShopOrder_OrderId(id)
        );

        return "account/order-detail";
    }
}
