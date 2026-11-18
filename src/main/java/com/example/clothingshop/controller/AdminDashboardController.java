package com.example.clothingshop.controller;

import com.example.clothingshop.repository.CategoryRepository;
import com.example.clothingshop.repository.ProductRepository;
import com.example.clothingshop.repository.ShopOrderRepository;
import com.example.clothingshop.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
public class AdminDashboardController {
    private final UserRepository users;
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final ShopOrderRepository orders;

    public AdminDashboardController(UserRepository users, ProductRepository products,
                                    CategoryRepository categories, ShopOrderRepository orders) {
        this.users = users;
        this.products = products;
        this.categories = categories;
        this.orders = orders;
    }

    @GetMapping({"/admin", "/admin/dashboard"})
    public String dashboard(Model model,
                            @RequestParam(required = false) Integer revenueYear,
                            @RequestParam(required = false) Integer revenueMonth) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime nextMonthStart = monthStart.plusMonths(1);

        BigDecimal monthlyRevenue = orders.calculateRevenue(monthStart, nextMonthStart);
        List<ShopOrderRepository.MonthlyRevenueView> revenueHistory = orders.findMonthlyRevenueHistory();

        int selectedYear = revenueYear == null ? now.getYear() : revenueYear;
        int selectedMonth = revenueMonth == null ? now.getMonthValue() : revenueMonth;
        BigDecimal selectedRevenue = BigDecimal.ZERO;
        if (selectedMonth >= 1 && selectedMonth <= 12 && selectedYear >= 2000 && selectedYear <= now.getYear()) {
            LocalDateTime selectedStart = LocalDate.of(selectedYear, selectedMonth, 1).atStartOfDay();
            selectedRevenue = orders.calculateRevenue(selectedStart, selectedStart.plusMonths(1));
        } else {
            selectedYear = now.getYear();
            selectedMonth = now.getMonthValue();
        }

        model.addAttribute("totalUsers", users.count());
        model.addAttribute("activeUsers", users.countByIsActiveTrue());
        model.addAttribute("customers", users.countByRole_RoleName("CUSTOMER"));
        model.addAttribute("products", products.count());
        model.addAttribute("categories", categories.count());
        model.addAttribute("orders", orders.count());
        model.addAttribute("pendingOrders", orders.countByOrderStatus("PENDING"));
        model.addAttribute("monthlyRevenue", monthlyRevenue == null ? BigDecimal.ZERO : monthlyRevenue);
        model.addAttribute("revenueHistory", revenueHistory);
        model.addAttribute("selectedRevenue", selectedRevenue == null ? BigDecimal.ZERO : selectedRevenue);
        model.addAttribute("selectedRevenueYear", selectedYear);
        model.addAttribute("selectedRevenueMonth", selectedMonth);
        model.addAttribute("currentYear", now.getYear());
        model.addAttribute("currentMonth", now.getMonthValue());
        return "admin/dashboard";
    }
}
