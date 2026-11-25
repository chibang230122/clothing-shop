package com.example.clothingshop.service;

import com.example.clothingshop.dto.ProfileForm;
import com.example.clothingshop.entity.Role;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.example.clothingshop.entity.User;
import com.example.clothingshop.repository.RoleRepository;
import com.example.clothingshop.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class AdminAccountService {
    private static final Set<String> FILTER_ROLES = Set.of("ADMIN", "STAFF", "CUSTOMER");
    private static final Set<String> EDITABLE_ROLES = Set.of("STAFF", "CUSTOMER");
    private final UserRepository users;
    private final RoleRepository roles;

    public AdminAccountService(UserRepository users, RoleRepository roles) {
        this.users = users;
        this.roles = roles;
    }

    @Transactional(readOnly = true)
    public Page<User> search(String keyword, String roleName, int page) {
        String search = keyword == null || keyword.trim().isEmpty() ? null : keyword.trim();
        String role = roleName == null || !FILTER_ROLES.contains(roleName) ? null : roleName;
        return users.searchAccounts(search, role,
                PageRequest.of(Math.max(0, page), 10, Sort.by(Sort.Direction.DESC, "userId")));
    }

    @Transactional(readOnly = true)
    public User getEditableProfile(Integer id) {
        return users.findById(id)
                .filter(user -> EDITABLE_ROLES.contains(user.getRole().getRoleName()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản có thể chỉnh sửa"));
    }

    @Transactional
    public void updateProfile(Integer id, ProfileForm form) {
        AccountService.applyProfile(getEditableProfile(id), form);
    }

    @Transactional
    public void setActive(Integer userId, String currentAdminEmail, boolean active) {
        User target = getEditable(userId, currentAdminEmail);
        target.setIsActive(active);
        users.save(target);
    }

    @Transactional
    public void changeRole(Integer userId, String currentAdminEmail, String roleName) {
        if (!EDITABLE_ROLES.contains(roleName)) {
            throw new IllegalArgumentException("Chỉ được phân quyền CUSTOMER hoặc STAFF tại đây");
        }
        User target = getEditable(userId, currentAdminEmail);
        Role role = roles.findByRoleName(roleName)
                .orElseThrow(() -> new IllegalStateException("Chưa có vai trò: " + roleName));
        target.setRole(role);
        users.save(target);
    }

    private User getEditable(Integer userId, String currentAdminEmail) {
        User user = users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));
        if (user.getEmail().equalsIgnoreCase(currentAdminEmail)) {
            throw new IllegalArgumentException("Không được sửa quyền hoặc trạng thái của chính mình");
        }
        if ("ADMIN".equals(user.getRole().getRoleName())) {
            throw new IllegalArgumentException("Không được sửa tài khoản ADMIN tại trang này");
        }
        return user;
    }
}