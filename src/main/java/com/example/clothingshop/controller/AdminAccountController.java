package com.example.clothingshop.controller;

import com.example.clothingshop.dto.ProfileForm;
import com.example.clothingshop.dto.RegisterForm;
import com.example.clothingshop.service.AuthService;
import com.example.clothingshop.service.PasswordPolicy;
import com.example.clothingshop.entity.User;
import com.example.clothingshop.service.AdminAccountService;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/accounts")
public class AdminAccountController {

    private final AdminAccountService accountService;

    private final AuthService authService;

    public AdminAccountController(
            AdminAccountService accountService, AuthService authService) {
        this.authService = authService;
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

    @GetMapping("/staff/new")
    public String newStaff(Model model) {
        model.addAttribute("form", new RegisterForm());
        return "admin/staff-create";
    }

    @PostMapping("/staff/new")
    public String createStaff(@Valid @ModelAttribute("form") RegisterForm form, BindingResult errors,
                              RedirectAttributes redirect) {
        if (!PasswordPolicy.accepts(form.getPassword())) {
            errors.rejectValue("password", "policy", PasswordPolicy.GUIDE);
        }
        if (form.getPassword() != null && !form.getPassword().equals(form.getConfirmPassword())) {
            errors.rejectValue("confirmPassword", "mismatch", "Mật khẩu xác nhận không khớp");
        }
        if (errors.hasErrors()) return "admin/staff-create";

        try {
            authService.createStaff(form);
        } catch (IllegalArgumentException ex) {
            errors.rejectValue("email", "registrationError", ex.getMessage());
            return "admin/staff-create";
        }
        redirect.addFlashAttribute("success", "Đã tạo tài khoản nhân viên.");
        return "redirect:/admin/accounts";
    }

    @GetMapping("/{id}/edit")
    public String editCustomer(@PathVariable Integer id, Model model) {
        User customer = accountService.getEditableProfile(id);
        model.addAttribute("customerEmail", customer.getEmail());
        model.addAttribute("customerId", id);
        model.addAttribute("form", new ProfileForm(customer.getFullName(), customer.getPhone(), customer.getAddress()));
        return "admin/customer-edit";
    }

    @PostMapping("/{id}/edit")
    public String updateCustomer(@PathVariable Integer id, @Valid @ModelAttribute("form") ProfileForm form,
                                 BindingResult errors, Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            model.addAttribute("customerEmail", accountService.getEditableProfile(id).getEmail());
            model.addAttribute("customerId", id);
            return "admin/customer-edit";
        }
        accountService.updateProfile(id, form);
        redirect.addFlashAttribute("success", "Đã cập nhật thông tin tài khoản.");
        return "redirect:/admin/accounts";
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
