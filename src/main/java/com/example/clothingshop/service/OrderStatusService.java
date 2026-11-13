package com.example.clothingshop.service;

import com.example.clothingshop.network.OrderStatusInfo;
import com.example.clothingshop.network.OrderStatusLookup;
import com.example.clothingshop.repository.ShopOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Tra cứu trạng thái đơn theo mã đơn cho Socket Server.
 * Chỉ trả về mã đơn, trạng thái đơn, trạng thái thanh toán và ngày đặt; không trả về tên, số điện thoại
 * hay địa chỉ của khách.
 */
@Service
public class OrderStatusService implements OrderStatusLookup {

    private final ShopOrderRepository shopOrderRepository;

    public OrderStatusService(ShopOrderRepository shopOrderRepository) {
        this.shopOrderRepository = shopOrderRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderStatusInfo> findOrderStatus(String orderCode) {
        return shopOrderRepository.findByOrderCode(orderCode)
                .map(order -> new OrderStatusInfo(
                        order.getOrderCode(),
                        order.getOrderStatus(),
                        order.getPaymentStatus(),
                        order.getOrderDate() == null ? null : order.getOrderDate().toString()));
    }
}
