package com.example.clothingshop;

import com.example.clothingshop.entity.Role;
import com.example.clothingshop.entity.User;
import com.example.clothingshop.repository.RoleRepository;
import com.example.clothingshop.repository.UserRepository;
import com.example.clothingshop.service.AdminAccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAccountServiceTests {
    @Mock UserRepository users;
    @Mock RoleRepository roles;
    @InjectMocks AdminAccountService service;

    private User account(int id, String email, String roleName) {
        Role role = new Role();
        role.setRoleName(roleName);
        User user = new User();
        user.setUserId(id);
        user.setEmail(email);
        user.setRole(role);
        user.setIsActive(true);
        return user;
    }

    @Test
    void customerCanBeDisabled() {
        User customer = account(2, "customer@test.local", "CUSTOMER");
        when(users.findById(2)).thenReturn(Optional.of(customer));
        service.setActive(2, "admin@test.local", false);
        assertFalse(customer.getIsActive());
        verify(users).save(customer);
    }

    @Test
    void adminCannotDisableOwnAccount() {
        User admin = account(1, "admin@test.local", "ADMIN");
        when(users.findById(1)).thenReturn(Optional.of(admin));
        assertThrows(IllegalArgumentException.class,
                () -> service.setActive(1, "admin@test.local", false));
        verify(users, never()).save(any(User.class));
    }

    @Test
    void adminCannotChangeAnotherAdminAccount() {
        User admin = account(3, "otheradmin@test.local", "ADMIN");
        when(users.findById(3)).thenReturn(Optional.of(admin));
        assertThrows(IllegalArgumentException.class,
                () -> service.changeRole(3, "admin@test.local", "CUSTOMER"));
        verify(users, never()).save(any(User.class));
    }

    @Test
    void noPromotionToAdminViaUserManagement() {
        assertThrows(IllegalArgumentException.class,
                () -> service.changeRole(2, "admin@test.local", "ADMIN"));
        verifyNoInteractions(users, roles);
    }

    @Test
    void customerCanBecomeStaff() {
        User customer = account(2, "customer@test.local", "CUSTOMER");
        Role staff = new Role();
        staff.setRoleName("STAFF");
        when(users.findById(2)).thenReturn(Optional.of(customer));
        when(roles.findByRoleName("STAFF")).thenReturn(Optional.of(staff));
        service.changeRole(2, "admin@test.local", "STAFF");
        assertEquals("STAFF", customer.getRole().getRoleName());
        verify(users).save(customer);
    }
}