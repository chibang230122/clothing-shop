package com.example.clothingshop.controller;

import com.example.clothingshop.dto.RegisterForm;
import com.example.clothingshop.service.AuthService;

import jakarta.validation.Valid;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String login(Authentication authentication) {

        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication
                instanceof AnonymousAuthenticationToken)) {
            return "redirect:/";
        }

        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("form", new RegisterForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("form") RegisterForm form,
            BindingResult result) {

        if (form.getPassword() != null
                && !form.getPassword()
                .equals(form.getConfirmPassword())) {
            result.rejectValue(
                    "confirmPassword",
                    "mismatch",
                    "Mật khẩu xác nhận không khớp"
            );
        }

        if (result.hasErrors()) {
            return "auth/register";
        }

        try {
            authService.registerCustomer(form);
        } catch (IllegalArgumentException ex) {
            result.rejectValue(
                    "email",
                    "registrationError",
                    ex.getMessage()
            );
            return "auth/register";
        }

        return "redirect:/login?registered";
    }

    @GetMapping("/access-denied")
    @ResponseStatus(org.springframework.http.HttpStatus.FORBIDDEN)
    public String accessDenied() {
        return "auth/access-denied";
    }
}
