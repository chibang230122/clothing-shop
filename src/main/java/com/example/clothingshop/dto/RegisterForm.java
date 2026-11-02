package com.example.clothingshop.dto;

import jakarta.validation.constraints.*;

public class RegisterForm {

    @NotBlank(message = "Vui lòng nhập họ tên")
    @Size(max = 100)
    private String fullName;

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không hợp lệ")
    @Size(max = 150)
    private String email;

    @Pattern(
            regexp = "^$|^[0-9+(). -]{9,20}$",
            message = "Số điện thoại không hợp lệ"
    )
    private String phone = "";

    @Size(max = 300, message = "Địa chỉ quá dài")
    private String address;

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    @Size(min = 8, max = 50,
            message = "Mật khẩu phải có từ 8 đến 50 ký tự")
    private String password;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu")
    private String confirmPassword;

    public String getFullName() { return fullName; }
    public void setFullName(String v) { fullName = v; }

    public String getEmail() { return email; }
    public void setEmail(String v) { email = v; }

    public String getPhone() { return phone; }
    public void setPhone(String v) { phone = v; }

    public String getAddress() { return address; }
    public void setAddress(String v) { address = v; }

    public String getPassword() { return password; }
    public void setPassword(String v) { password = v; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String v) {
        confirmPassword = v;
    }
}
