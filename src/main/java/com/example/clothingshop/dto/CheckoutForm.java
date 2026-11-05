package com.example.clothingshop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CheckoutForm {

    @NotBlank(message = "Vui lòng nhập tên người nhận.")
    @Size(max = 100, message = "Tên người nhận không được quá 100 ký tự.")
    private String recipientName;

    @NotBlank(message = "Vui lòng nhập số điện thoại.")
    @Pattern(
            regexp = "^[0-9+(). -]{9,20}$",
            message = "Số điện thoại không hợp lệ."
    )
    private String recipientPhone;

    @NotBlank(message = "Vui lòng nhập địa chỉ giao hàng.")
    @Size(max = 300, message = "Địa chỉ không được quá 300 ký tự.")
    private String shippingAddress;

    @NotBlank(message = "Vui lòng chọn phương thức thanh toán.")
    @Pattern(
            regexp = "COD|BANK_TRANSFER|E_WALLET",
            message = "Phương thức thanh toán không hợp lệ."
    )
    private String paymentMethod = "COD";

    @Size(max = 500, message = "Ghi chú không được quá 500 ký tự.")
    private String note;

    public CheckoutForm() {
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}