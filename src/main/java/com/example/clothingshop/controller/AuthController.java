package com.example.clothingshop.controller;

import com.example.clothingshop.dto.RegisterForm;
import com.example.clothingshop.service.AuthService;

import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import com.example.clothingshop.service.PasswordPolicy;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    private final AuthService authService;
    private final ObjectProvider<ClientRegistrationRepository> clients;

    public AuthController(AuthService authService, ObjectProvider<ClientRegistrationRepository> clients) {
        this.authService = authService;
        this.clients = clients;
    }

    @ModelAttribute("googleEnabled")
    public boolean googleEnabled() {
        ClientRegistrationRepository registrations = clients.getIfAvailable();
        return registrations != null && registrations.findByRegistrationId("google") != null;
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

        if (!result.hasFieldErrors("password") && !PasswordPolicy.accepts(form.getPassword())) {
            result.rejectValue("password", "policy", PasswordPolicy.GUIDE);
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
