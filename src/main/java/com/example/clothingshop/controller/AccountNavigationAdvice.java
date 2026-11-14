package com.example.clothingshop.controller;

import com.example.clothingshop.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class AccountNavigationAdvice {

    private final UserRepository users;

    public AccountNavigationAdvice(UserRepository users) {
        this.users = users;
    }

    @ModelAttribute("signedInAccount")
    public AccountLabel signedInAccount(Authentication authentication) {
        if (authentication == null) {
            return null;
        }

        return users.findByEmailIgnoreCase(authentication.getName())
                .map(user -> new AccountLabel(user.getFullName(), user.getRole().getRoleName()))
                .orElse(null);
    }

    public record AccountLabel(String fullName, String role) {}
}
