package com.example.clothingshop.controller;

import com.example.clothingshop.repository.VoucherRepository;
import com.example.clothingshop.service.VoucherService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.LocalDateTime;
import java.util.Collections;

@ControllerAdvice
public class VoucherAdvice {

    private final VoucherRepository voucherRepository;
    private final VoucherService voucherService;

    public VoucherAdvice(VoucherRepository voucherRepository, VoucherService voucherService) {
        this.voucherRepository = voucherRepository;
        this.voucherService = voucherService;
    }

    @ModelAttribute("shopVouchers")
    public Object shopVouchers(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith("/checkout")) {
            return voucherRepository.findAvailableVouchers(LocalDateTime.now());
        }
        return Collections.emptyList();
    }

    @ModelAttribute("appliedVoucherCode")
    public String appliedVoucherCode(HttpServletRequest request) {
        String code = request.getParameter("voucherCode");
        return (code != null && !code.trim().isEmpty()) ? code.trim().toUpperCase() : null;
    }

    @ModelAttribute("voucherService")
    public VoucherService voucherService() {
        return voucherService;
    }
}