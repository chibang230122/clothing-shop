package com.example.clothingshop.controller;

import com.example.clothingshop.entity.ShopOrder;
import com.example.clothingshop.repository.ShopOrderRepository;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/account/orders")
public class CustomerOrderController {

    private final ShopOrderRepository orderRepository;

    public CustomerOrderController(
            ShopOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // Danh sách chỉ chứa đơn hàng của tài khoản hiện tại
    @GetMapping
    public String myOrders(
            Principal principal, Model model) {

        model.addAttribute(
                "orders",
                orderRepository
                        .findByUser_EmailIgnoreCaseOrderByOrderDateDesc(
                                principal.getName()
                        )
        );

        return "account/orders";
    }

    // Chỉ trả về đơn hàng về người dùng
    @GetMapping("/{id}")
    public String orderDetail(
            @PathVariable Long id,
            Principal principal,
            Model model) {

        ShopOrder order = orderRepository
                .findByOrderIdAndUser_EmailIgnoreCase(
                        id, principal.getName()
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy đơn hàng"
                        )
                );

        model.addAttribute("order", order);

        return "account/order-detail";
    }
}
