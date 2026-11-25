package com.example.clothingshop.service;

import com.example.clothingshop.entity.User;
import com.example.clothingshop.repository.RoleRepository;
import com.example.clothingshop.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class GoogleLoginService extends OidcUserService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final TransactionTemplate transactions;

    public GoogleLoginService(UserRepository users, RoleRepository roles, TransactionTemplate transactions) {
        this.users = users;
        this.roles = roles;
        this.transactions = transactions;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest request) {
        return authenticateIdentity(super.loadUser(request));
    }

    OidcUser authenticateIdentity(OidcUser identity) {
        if (!Boolean.TRUE.equals(identity.getEmailVerified()) || identity.getEmail() == null
                || identity.getEmail().isBlank() || identity.getEmail().length() > 150) {
            throw denied();
        }
        try {
            return transactions.execute(status -> resolve(identity));
        } catch (DataIntegrityViolationException ex) {
            throw denied();
        }
    }

    private OidcUser resolve(OidcUser identity) {
        String email = identity.getEmail().toLowerCase(Locale.ROOT);
        User user = users.findByGoogleSubject(identity.getSubject()).orElseGet(() -> {
            if (users.existsByEmailIgnoreCase(email)) {
                throw denied();
            }
            User customer = new User();
            customer.setRole(roles.findByRoleName("CUSTOMER").orElseThrow());
            customer.setEmail(email);
            String name = identity.getFullName();
            if (name == null || name.isBlank()) name = email;
            customer.setFullName(name.substring(0, Math.min(name.length(), 100)));
            customer.setGoogleSubject(identity.getSubject());
            customer.setPasswordHash("!"); // Tài khoản mới chỉ đăng nhập qua Google.
            customer.setCreatedAt(LocalDateTime.now());
            return users.saveAndFlush(customer);
        });
        if (!Boolean.TRUE.equals(user.getIsActive()) || !email.equalsIgnoreCase(user.getEmail())) {
            throw denied();
        }
        return new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().getRoleName())),
                identity.getIdToken(), identity.getUserInfo(), "email");
    }

    private static OAuth2AuthenticationException denied() {
        return new OAuth2AuthenticationException(new OAuth2Error("account_unavailable"));
    }
}
