package com.example.clothingshop.controller;

import com.example.clothingshop.service.PasswordResetService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.clothingshop.service.PasswordPolicy;
import java.security.Principal;
import java.util.Locale;

@Controller
public class PasswordResetController {

    private static final String RESET_EMAIL = PasswordResetController.class.getName() + ".email";

    private final PasswordResetService passwordReset;
    private final Validator validator;

    public PasswordResetController(PasswordResetService passwordReset, Validator validator) {
        this.passwordReset = passwordReset;
        this.validator = validator;
    }

    @GetMapping("/forgot-password")
    public String forgotPassword(Principal principal, Model model) {
        if (principal != null) {
            return "redirect:/account/change-password";
        }
        model.addAttribute("mailAvailable", passwordReset.isAvailable());
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String requestCode(@RequestParam String email, Principal principal,
                              HttpSession session, Model model) {
        if (principal != null) {
            return "redirect:/account/change-password";
        }
        if (!passwordReset.isAvailable()) {
            return "redirect:/forgot-password";
        }

        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (!validator.validate(new EmailAddress(normalized)).isEmpty()) {
            model.addAttribute("mailAvailable", true);
            model.addAttribute("error", "Vui lòng nhập email hợp lệ.");
            return "auth/forgot-password";
        }

        passwordReset.requestCode(normalized, session.getId());
        session.setAttribute(RESET_EMAIL, normalized);
        return "redirect:/reset-password";
    }

    @GetMapping("/reset-password")
    public String resetForm(Principal principal, HttpSession session, Model model) {
        if (principal != null) {
            return "redirect:/account/change-password";
        }
        String email = (String) session.getAttribute(RESET_EMAIL);
        if (email == null) {
            return "redirect:/forgot-password";
        }
        model.addAttribute("resetEmail", email);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String reset(@RequestParam String otp, @RequestParam String newPassword,
                        @RequestParam String confirmPassword, Principal principal,
                        HttpSession session, Model model) {
        if (principal != null) {
            return "redirect:/account/change-password";
        }
        String email = (String) session.getAttribute(RESET_EMAIL);
        if (email == null) {
            return "redirect:/forgot-password";
        }

        String error;
        if (!PasswordPolicy.accepts(newPassword)) {
            error = PasswordPolicy.GUIDE;
        } else if (!newPassword.equals(confirmPassword)) {
            error = "Mật khẩu xác nhận không khớp.";
        } else if (!otp.matches("[0-9]{6}") || !passwordReset.resetPassword(email, session.getId(), otp, newPassword)) {
            error = "Mã OTP không hợp lệ hoặc đã hết hiệu lực. Bạn có thể yêu cầu mã mới.";
        } else {
            session.invalidate();
            return "redirect:/login?reset";
        }

        model.addAttribute("resetEmail", email);
        model.addAttribute("error", error);
        return "auth/reset-password";
    }

    private record EmailAddress(@NotBlank @Email @Size(max = 150) String value) {}
}
