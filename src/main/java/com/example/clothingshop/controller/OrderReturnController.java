package com.example.clothingshop.controller;
import com.example.clothingshop.service.OrderReturnService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;
@Controller
public class OrderReturnController {
 private final OrderReturnService service;
 public OrderReturnController(OrderReturnService service){this.service=service;}
 @PostMapping("/account/orders/{id}/return") public String request(@PathVariable Long id,Principal principal,RedirectAttributes ra){try{service.requestReturn(id,principal.getName());ra.addFlashAttribute("successMessage","Đã gửi yêu cầu trả hàng.");}catch(Exception e){ra.addFlashAttribute("errorMessage",e.getMessage());}return "redirect:/account/orders/"+id;}
 @PostMapping("/staff/orders/{id}/return-received") public String received(@PathVariable Long id,RedirectAttributes ra){try{service.receiveReturn(id);ra.addFlashAttribute("successMessage","Đã xác nhận nhận hàng trả.");}catch(Exception e){ra.addFlashAttribute("errorMessage",e.getMessage());}return "redirect:/staff/orders/"+id;}
 @PostMapping("/staff/orders/{id}/refund") public String refund(@PathVariable Long id,Principal principal,RedirectAttributes ra){try{service.confirmRefundAndRestock(id,principal.getName());ra.addFlashAttribute("successMessage","Đã xác nhận hoàn tiền và cập nhật tồn kho.");}catch(Exception e){ra.addFlashAttribute("errorMessage",e.getMessage());}return "redirect:/staff/orders/"+id;}
}
