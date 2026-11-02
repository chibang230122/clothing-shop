package com.example.clothingshop.dto;

import jakarta.validation.constraints.*;

public class ChangePasswordForm {

    @NotBlank(message = "Nhập mật khẩu hiện tại")
    private String currentPassword;

    @NotBlank(message = "Nhập mật khẩu mới")
    @Size(min = 8, max = 50,
            message = "Mật khẩu mới phải có 8-50 ký tự")
    private String newPassword;

    @NotBlank(message = "Xác nhận mật khẩu mới")
    private String confirmPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String value) {
        currentPassword = value;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String value) {
        newPassword = value;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String value) {
        confirmPassword = value;
    }
}
