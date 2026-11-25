package com.example.clothingshop.service;

import com.example.clothingshop.entity.ShopOrder;
import com.example.clothingshop.repository.OrderItemRepository;
import com.example.clothingshop.repository.ShopOrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class InvoiceService {
    private final ShopOrderRepository orders;
    private final OrderItemRepository items;
    private final SpringTemplateEngine templates;

    public InvoiceService(ShopOrderRepository orders, OrderItemRepository items, SpringTemplateEngine templates) {
        this.orders = orders;
        this.items = items;
        this.templates = templates;
    }

    public record Invoice(String email, String filename, byte[] content) {}

    @Transactional(readOnly = true)
    public Invoice export(long id) {
        ShopOrder order = orders.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Context context = new Context(Locale.forLanguageTag("vi-VN"));
        context.setVariable("order", order);
        context.setVariable("items", items.findByShopOrder_OrderId(id));
        String html = templates.process("invoice/document", context);
        return new Invoice(order.getUser().getEmail(), "hoa-don-" + id + ".html", html.getBytes(StandardCharsets.UTF_8));
    }
}
