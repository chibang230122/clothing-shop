package com.example.clothingshop.network;

/** Thông tin trạng thái đơn hàng trả về cho client. Cố ý KHÔNG chứa thông tin cá nhân. */
public record OrderStatusInfo(String orderCode,
                              String orderStatus,
                              String paymentStatus,
                              String orderDate) {
}
