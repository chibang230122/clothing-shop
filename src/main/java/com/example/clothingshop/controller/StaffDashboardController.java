package com.example.clothingshop.controller;

import com.example.clothingshop.repository.ShopOrderRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StaffDashboardController {

    private final ShopOrderRepository orderRepository;

    public StaffDashboardController(ShopOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @GetMapping("/staff/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalOrders", orderRepository.count());
        model.addAttribute("pendingOrders", orderRepository.countByOrderStatus("PENDING"));
        model.addAttribute("confirmedOrders", orderRepository.countByOrderStatus("CONFIRMED"));
        model.addAttribute("processingOrders", orderRepository.countByOrderStatus("PROCESSING"));
        model.addAttribute("shippingOrders", orderRepository.countByOrderStatus("SHIPPING"));
        model.addAttribute("completedOrders", orderRepository.countByOrderStatus("COMPLETED"));
        model.addAttribute("cancelledOrders", orderRepository.countByOrderStatus("CANCELLED"));
        return "staff/dashboard";
    }
}
