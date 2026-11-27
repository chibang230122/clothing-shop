package com.example.clothingshop.controller;

import com.example.clothingshop.entity.ShopOrder;
import com.example.clothingshop.repository.OrderItemRepository;
import com.example.clothingshop.repository.ShopOrderRepository;
import com.example.clothingshop.service.OrderManagementService;
import com.example.clothingshop.service.OrderPaymentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/staff/orders")
public class StaffOrderController {

    private final ShopOrderRepository shopOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderManagementService orderManagementService;
    private final OrderPaymentService orderPaymentService;

    public StaffOrderController(
            ShopOrderRepository shopOrderRepository,
            OrderItemRepository orderItemRepository,
            OrderManagementService orderManagementService,
            OrderPaymentService orderPaymentService) {
        this.shopOrderRepository = shopOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderManagementService = orderManagementService;
        this.orderPaymentService = orderPaymentService;
    }

    @GetMapping
    public String orderList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        LocalDateTime from;
        LocalDateTime to;
        try {
            from = parseDate(fromDate, false);
            to = parseDate(toDate, true);
        } catch (Exception ex) {
            model.addAttribute("dateError", "Ngày lọc không hợp lệ. Vui lòng chọn lại.");
            from = null;
            to = null;
        }
        if (from != null && to != null && !from.isBefore(to)) {
            model.addAttribute("dateError", "Ngày bắt đầu phải nhỏ hơn hoặc bằng ngày kết thúc.");
            from = null;
            to = null;
        }

        Page<ShopOrder> orders = shopOrderRepository.searchStaffOrders(
                blankToNull(keyword), blankToNull(status), from, to,
                PageRequest.of(Math.max(page, 0), 10,
                        Sort.by(Sort.Direction.DESC, "orderDate").and(Sort.by(Sort.Direction.DESC, "orderId"))));

        model.addAttribute("orders", orders);
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("status", status == null ? "" : status);
        model.addAttribute("fromDate", fromDate == null ? "" : fromDate);
        model.addAttribute("toDate", toDate == null ? "" : toDate);
        return "staff/orders";
    }

    @GetMapping("/{orderId}")
    public String orderDetail(@PathVariable Long orderId, Model model) {
        ShopOrder order = shopOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng."));
        model.addAttribute("order", order);
        model.addAttribute("orderItems", orderItemRepository.findByShopOrder_OrderId(orderId));
        return "staff/order-detail";
    }

    @PostMapping("/{orderId}/status")
    public String updateStatus(
            @PathVariable Long orderId,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {
        try {
            orderManagementService.updateOrderStatus(orderId, status);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái đơn hàng thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    e.getMessage() == null ? "Cập nhật trạng thái thất bại." : e.getMessage());
        }
        return "redirect:/staff/orders";
    }

    @PostMapping("/{orderId}/payment")
    public String confirmPayment(@PathVariable Long orderId, RedirectAttributes ra) {
        try { orderPaymentService.confirmPayment(orderId); ra.addFlashAttribute("successMessage", "Đã xác nhận nhận thanh toán."); }
        catch (Exception e) { ra.addFlashAttribute("errorMessage", e.getMessage()); }
        return "redirect:/staff/orders/" + orderId;
    }

    private LocalDateTime parseDate(String value, boolean endOfDay) {
        if (value == null || value.isBlank()) return null;
        LocalDate date = LocalDate.parse(value);
        return endOfDay ? date.plusDays(1).atStartOfDay() : date.atStartOfDay();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
