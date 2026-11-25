package com.example.clothingshop.service;

import com.example.clothingshop.entity.Role;
import com.example.clothingshop.repository.RoleRepository;
import com.example.clothingshop.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("google")
@SpringBootTest(properties = {
        "GOOGLE_CLIENT_ID=test-client", "GOOGLE_CLIENT_SECRET=test-secret",
        "spring.datasource.url=jdbc:h2:mem:google-login;MODE=MSSQLServer",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.admin.seed.enabled=false", "shop.socket.enabled=false"
})
class GoogleLoginTests {
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;
    @Autowired GoogleLoginService login;
    @Autowired RoleRepository roles;
    @Autowired UserRepository users;

    @Test
    void googleButtonStartsAuthorizationCodeFlow() throws Exception {
        var loginPage = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/login"))
                .andReturn().getResponse();
        assertTrue(loginPage.getContentAsString().contains("/oauth2/authorization/google"));
        var registerPage = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/register"))
                .andReturn().getResponse();
        assertTrue(registerPage.getContentAsString(java.nio.charset.StandardCharsets.UTF_8)
                .contains("Đăng ký bằng Google"));
        assertTrue(registerPage.getContentAsString().contains("href=\"/oauth2/authorization/google\""));
        var response = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/oauth2/authorization/google")).andReturn().getResponse();
        assertEquals(302, response.getStatus());
        String redirect = response.getRedirectedUrl();
        assertNotNull(redirect);
        assertTrue(redirect.startsWith("https://accounts.google.com/"));
        assertTrue(redirect.contains("state="));
        assertTrue(redirect.contains("nonce="));
    }

    @Test
    void verifiedSubjectKeepsItsAccountAndCannotClaimAnotherAccount() {
        Role customer = new Role();
        customer.setRoleName("CUSTOMER");
        roles.save(customer);

        assertThrows(OAuth2AuthenticationException.class, () -> login.authenticateIdentity(identity("123", false)));
        var principal = login.authenticateIdentity(identity("123", true));
        assertEquals("buyer@example.test", principal.getName());
        assertTrue(principal.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        login.authenticateIdentity(identity("123", true));
        assertEquals(1, users.count());
        assertThrows(OAuth2AuthenticationException.class, () -> login.authenticateIdentity(identity("456", true)));
        var user = users.findByGoogleSubject("123").orElseThrow();
        user.setIsActive(false);
        users.save(user);
        assertThrows(OAuth2AuthenticationException.class, () -> login.authenticateIdentity(identity("123", true)));
    }

    private OidcUser identity(String subject, boolean verified) {
        var token = new OidcIdToken("test", Instant.now(), Instant.now().plusSeconds(300),
                Map.of("sub", subject, "email", "buyer@example.test", "email_verified", verified));
        return new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")), token);
    }
}
