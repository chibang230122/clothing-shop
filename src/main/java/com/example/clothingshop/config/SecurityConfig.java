package com.example.clothingshop.config;

import com.example.clothingshop.service.CustomUserDetailsService;
import org.springframework.beans.factory.ObjectProvider;
import com.example.clothingshop.service.GoogleLoginService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher sessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    // Dung dung UserDetailsService doc tu SQL Server, ca trong kiem thu MVC.
    @Bean
    public AuthenticationProvider authenticationProvider(
            CustomUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, AuthenticationProvider authenticationProvider,
            CustomUserDetailsService userDetailsService, SessionRegistry sessions,
            ObjectProvider<GoogleLoginService> google,
            ObjectProvider<ClientRegistrationRepository> clients) throws Exception {
        http.authenticationProvider(authenticationProvider)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/products", "/products/**", "/login", "/register",
                                "/forgot-password", "/reset-password", "/access-denied",
                                "/error", "/css/**", "/js/**",
                                "/images/**", "/favicon.ico").permitAll()
                        .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                        .requestMatchers("/staff", "/staff/**", "/api/invoices/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/account/orders", "/account/orders/**").hasRole("CUSTOMER")
                        .requestMatchers("/cart", "/cart/**", "/checkout", "/checkout/**",
                                "/orders", "/orders/**")
                        .access(AuthorizationManagers.allOf(
                                AuthorityAuthorizationManager.hasRole("CUSTOMER"),
                                (authentication, context) -> new AuthorizationDecision(
                                        userDetailsService.isActiveCustomer(authentication.get().getName()))
                        ))
                        .requestMatchers("/account/**").authenticated()
                        .anyRequest().denyAll()
                )
                .sessionManagement(session -> session.maximumSessions(-1)
                        .sessionRegistry(sessions).expiredUrl("/login?expired"))
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler((request, response, authentication) -> {
                            boolean admin = authentication.getAuthorities().stream()
                                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
                            SavedRequestAwareAuthenticationSuccessHandler handler =
                                    new SavedRequestAwareAuthenticationSuccessHandler();
                            handler.setDefaultTargetUrl(admin ? "/admin" : "/");
                            handler.onAuthenticationSuccess(request, response, authentication);
                        })
                        .failureHandler((request, response, exception) -> {
                            if (exception instanceof DisabledException || exception instanceof LockedException) {
                                response.sendRedirect(request.getContextPath() + "/login?locked");
                            } else {
                                response.sendRedirect(request.getContextPath() + "/login?error");
                            }
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .exceptionHandling(ex -> ex.accessDeniedPage("/access-denied"));
        if (clients.getIfAvailable() != null && clients.getObject().findByRegistrationId("google") != null) {
            http.oauth2Login(login -> login.loginPage("/login")
                    .userInfoEndpoint(info -> info.oidcUserService(google.getObject()))
                    .failureUrl("/login?googleError"));
        }
        return http.build();
    }
}