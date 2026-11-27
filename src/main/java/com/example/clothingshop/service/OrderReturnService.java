package com.example.clothingshop.service;

import com.example.clothingshop.entity.*;
import com.example.clothingshop.repository.OrderItemRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class OrderReturnService {
    @PersistenceContext private EntityManager em;
    private final OrderItemRepository items;
    public OrderReturnService(OrderItemRepository items){this.items=items;}
    @Transactional
    public void requestReturn(Long orderId,String email){
        ShopOrder order=em.find(ShopOrder.class,orderId,LockModeType.PESSIMISTIC_WRITE);
        if(order==null || !order.getUser().getEmail().equalsIgnoreCase(email)) throw new IllegalArgumentException("Không tìm thấy đơn hàng.");
        if(!"COMPLETED".equals(order.getOrderStatus())) throw new IllegalStateException("Chỉ được yêu cầu trả hàng với đơn đã giao thành công.");
        order.setOrderStatus("RETURN_REQUESTED");
    }
    @Transactional
    public void receiveReturn(Long orderId){
        ShopOrder order=locked(orderId);
        if(!"RETURN_REQUESTED".equals(order.getOrderStatus())) throw new IllegalStateException("Đơn hàng chưa có yêu cầu trả hàng.");
        order.setOrderStatus("RETURN_RECEIVED");
    }
    @Transactional
    public void confirmRefundAndRestock(Long orderId,String email){
        ShopOrder order=locked(orderId);
        if(!"RETURN_RECEIVED".equals(order.getOrderStatus())) throw new IllegalStateException("Chỉ xác nhận khi đã nhận được hàng trả.");
        if(!"PAID".equals(order.getPaymentStatus())) throw new IllegalStateException("Đơn chưa được thanh toán. Kiểm tra thanh toán trước khi xác nhận hoàn tiền.");
        User staff=em.createQuery("select u from User u where lower(u.email)=lower(:email)",User.class).setParameter("email",email).getSingleResult();
        List<OrderItem> orderItems=items.findByShopOrder_OrderId(orderId);
        for(OrderItem item:orderItems){
            ProductVariant variant=em.find(ProductVariant.class,item.getProductVariant().getVariantId(),LockModeType.PESSIMISTIC_WRITE);
            if(variant==null) throw new IllegalStateException("Không tìm thấy biến thể sản phẩm.");
            int before=variant.getStockQuantity(); int after=Math.addExact(before,item.getQuantity()); variant.setStockQuantity(after);
            em.persist(new StockTransaction(variant,"RETURN",item.getQuantity(),before,after,order.getOrderCode(),"Hoàn hàng từ đơn "+order.getOrderCode(),staff));
        }
        order.setOrderStatus("RETURNED"); order.setPaymentStatus("REFUNDED");
    }
    private ShopOrder locked(Long id){ShopOrder order=em.find(ShopOrder.class,id,LockModeType.PESSIMISTIC_WRITE); if(order==null)throw new IllegalArgumentException("Không tìm thấy đơn hàng.");return order;}
}
