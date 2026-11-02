package com.example.clothingshop.controller;

import com.example.clothingshop.repository.CategoryRepository;
import com.example.clothingshop.repository.ProductRepository;
import com.example.clothingshop.repository.ShopOrderRepository;
import com.example.clothingshop.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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

    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAttribute("totalUsers", users.count());
        model.addAttribute("activeUsers", users.countByIsActiveTrue());
        model.addAttribute("customers", users.countByRole_RoleName("CUSTOMER"));
        model.addAttribute("products", products.count());
        model.addAttribute("categories", categories.count());
        model.addAttribute("orders", orders.count());
        return "admin/dashboard";
    }
}