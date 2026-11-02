package com.example.clothingshop.controller;

import com.example.clothingshop.dto.ChangePasswordForm;
import com.example.clothingshop.dto.ProfileForm;
import com.example.clothingshop.entity.User;
import com.example.clothingshop.service.AccountService;

import jakarta.validation.Valid;

import java.security.Principal;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/profile")
    public String profile(Model model, Principal principal) {

        User user = accountService
                .getCurrentUser(principal.getName());

        model.addAttribute("email", user.getEmail());

        model.addAttribute("form", new ProfileForm(
                user.getFullName(),
                user.getPhone(),
                user.getAddress()
        ));

        return "account/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @Valid @ModelAttribute("form") ProfileForm form,
            BindingResult result,
            Model model,
            Principal principal) {

        if (result.hasErrors()) {
            model.addAttribute("email", principal.getName());
            return "account/profile";
        }

        accountService.updateProfile(
                principal.getName(), form
        );

        return "redirect:/account/profile?success";
    }

    @GetMapping("/change-password")
    public String changePasswordPage(Model model) {

        model.addAttribute(
                "form", new ChangePasswordForm()
        );

        return "account/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(
            @Valid @ModelAttribute("form")
            ChangePasswordForm form,
            BindingResult result,
            Principal principal) {

        if (form.getNewPassword() != null
                && !form.getNewPassword()
                .equals(form.getConfirmPassword())) {

            result.rejectValue(
                    "confirmPassword",
                    "mismatch",
                    "Mật khẩu xác nhận không khớp"
            );
        }

        if (form.getNewPassword() != null
                && form.getNewPassword()
                .getBytes(StandardCharsets.UTF_8).length > 72) {
            result.rejectValue(
                    "newPassword",
                    "tooLong",
                    "Mật khẩu vượt quá giới hạn BCrypt"
            );
        }

        if (result.hasErrors()) {
            return "account/change-password";
        }

        try {
            accountService.changePassword(
                    principal.getName(),
                    form.getCurrentPassword(),
                    form.getNewPassword()
            );
        } catch (IllegalArgumentException ex) {
            result.rejectValue(
                    "currentPassword",
                    "invalid",
                    ex.getMessage()
            );
            return "account/change-password";
        }

        return "redirect:/account/change-password?success";
    }
}
