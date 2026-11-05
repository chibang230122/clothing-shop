
package com.example.clothingshop.controller;

import com.example.clothingshop.repository.ShopOrderRepository;
import com.example.clothingshop.service.OrderManagementService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/staff/orders")
public class StaffOrderController {

    private final ShopOrderRepository shopOrderRepository;
    private final OrderManagementService orderManagementService;

    public StaffOrderController(
            ShopOrderRepository shopOrderRepository,
            OrderManagementService orderManagementService) {
        this.shopOrderRepository = shopOrderRepository;
        this.orderManagementService = orderManagementService;
    }

    @GetMapping
    public String orderList(Model model) {
        model.addAttribute("orders", shopOrderRepository.findAll());
        return "staff/orders";
    }

    @PostMapping("/{orderId}/status")
    public String updateStatus(
            @PathVariable Long orderId,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {

        try {
            orderManagementService.updateOrderStatus(orderId, status);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Cập nhật trạng thái đơn hàng thành công"
            );
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/staff/orders";
    }
}
