package com.example.clothingshop.dto;

import jakarta.validation.constraints.*;

public class ChangePasswordForm {

    @NotBlank(message = "Nhập mật khẩu hiện tại")
    private String currentPassword;

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
