package com.example.clothingshop.dto;

import jakarta.validation.constraints.*;

public class ProfileForm {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100)
    private String fullName;

    @Pattern(
            regexp = "^$|^[0-9+(). -]{9,20}$",
            message = "Số điện thoại không hợp lệ"
    )
    private String phone = "";

    @Size(max = 300, message = "Địa chỉ quá dài")
    private String address;

    public ProfileForm() {}

    public ProfileForm(String fullName, String phone, String address) {
        this.fullName = fullName;
        this.phone = phone == null ? "" : phone;
        this.address = address;
    }

    public String getFullName() { return fullName; }
    public void setFullName(String v) { fullName = v; }

    public String getPhone() { return phone; }
    public void setPhone(String v) { phone = v; }

    public String getAddress() { return address; }
    public void setAddress(String v) { address = v; }
}
