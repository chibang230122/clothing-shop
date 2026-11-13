package com.example.clothingshop.network;

import java.util.Optional;

/** Tra cứu trạng thái đơn hàng theo mã đơn. */
public interface OrderStatusLookup {
    Optional<OrderStatusInfo> findOrderStatus(String orderCode);
}
