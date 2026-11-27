package com.example.clothingshop.controller;

import com.example.clothingshop.repository.StockTransactionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class StaffStockTransactionController {
    private final StockTransactionRepository repository;

    public StaffStockTransactionController(StockTransactionRepository repository) {
        this.repository = repository;
    }

    // URL duoc dung chung cho ADMIN va STAFF; phan quyen da duoc SecurityConfig bao ve.
    @GetMapping({"/staff/stock-transactions", "/admin/stock-transactions"})
    public String history(@RequestParam(defaultValue = "0") int page,
                          @RequestParam(required = false) String type, Model model) {
        String normalizedType = (type == null || type.isBlank() || "ALL".equalsIgnoreCase(type))
                ? null : type.trim().toUpperCase();
        if (normalizedType != null && !java.util.Set.of("IMPORT", "SALE", "RETURN", "ADJUSTMENT").contains(normalizedType)) {
            normalizedType = null;
        }
        model.addAttribute("transactions", repository.findHistory(normalizedType,
                PageRequest.of(Math.max(0, page), 25)));
        model.addAttribute("selectedType", normalizedType == null ? "ALL" : normalizedType);
        return "staff/stock-transactions";
    }
}
