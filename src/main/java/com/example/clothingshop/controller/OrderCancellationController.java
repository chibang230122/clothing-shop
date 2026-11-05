
package com.example.clothingshop.controller;

import com.example.clothingshop.service.OrderCancellationService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class OrderCancellationController {

    private final OrderCancellationService orderCancellationService;

    public OrderCancellationController(
            OrderCancellationService orderCancellationService) {
        this.orderCancellationService = orderCancellationService;
    }

    @PostMapping("/account/orders/{orderId}/cancel")
    public String cancelOrder(
            @PathVariable Long orderId,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {
            orderCancellationService.cancelOrder(
                    orderId,
                    principal.getName()
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Hủy đơn hàng thành công"
            );

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/account/orders/" + orderId;
    }
}
