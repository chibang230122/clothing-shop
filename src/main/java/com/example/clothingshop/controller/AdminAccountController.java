package com.example.clothingshop.controller;

import com.example.clothingshop.service.AdminAccountService;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/accounts")
public class AdminAccountController {

    private final AdminAccountService accountService;

    public AdminAccountController(
            AdminAccountService accountService) {
        this.accountService = accountService;
    }

    // 1. Hiển thị danh sách tài khoản
    @GetMapping
    public String listAccounts(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(name = "role", defaultValue = "")
            String role,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        model.addAttribute(
                "accountPage",
                accountService.search(keyword, role, page)
        );

        model.addAttribute("keyword", keyword);
        model.addAttribute("roleFilter", role);

        return "admin/accounts";
    }

    // 2. Khóa hoặc mở khóa tài khoản
    @PostMapping("/{id}/status")
    public String changeStatus(
            @PathVariable Integer id,
            @RequestParam boolean active,
            Principal principal,
            RedirectAttributes redirect) {

        try {
            accountService.setActive(
                    id,
                    principal.getName(),
                    active
            );

            redirect.addFlashAttribute(
                    "success",
                    active
                            ? "Mở khóa tài khoản thành công!"
                            : "Khóa tài khoản thành công!"
            );

        } catch (IllegalArgumentException |
                 IllegalStateException ex) {

            redirect.addFlashAttribute(
                    "error", ex.getMessage()
            );
        }

        return "redirect:/admin/accounts";
    }

    // 3. Thay đổi vai trò Customer / Staff
    @PostMapping("/{id}/role")
    public String changeRole(
            @PathVariable Integer id,
            @RequestParam String roleName,
            Principal principal,
            RedirectAttributes redirect) {

        try {
            accountService.changeRole(
                    id,
                    principal.getName(),
                    roleName
            );

            redirect.addFlashAttribute(
                    "success",
                    "Cập nhật vai trò thành công!"
            );

        } catch (IllegalArgumentException |
                 IllegalStateException ex) {

            redirect.addFlashAttribute(
                    "error", ex.getMessage()
            );
        }

        return "redirect:/admin/accounts";
    }
}
