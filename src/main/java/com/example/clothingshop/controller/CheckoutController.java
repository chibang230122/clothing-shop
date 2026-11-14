
package com.example.clothingshop.controller;

import com.example.clothingshop.dto.CheckoutForm;
import com.example.clothingshop.entity.CartItem;
import com.example.clothingshop.entity.ShopOrder;
import com.example.clothingshop.service.CartService;
import com.example.clothingshop.service.OrderService;

import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    private final CartService cartService;
    private final OrderService orderService;

    public CheckoutController(
            CartService cartService,
            OrderService orderService) {

        this.cartService = cartService;
        this.orderService = orderService;
    }

    @GetMapping
    public String showCheckout(
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {

        List<CartItem> cartItems =
                cartService.getCartItems(principal.getName());

        if (cartItems.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Giỏ hàng đang trống, không thể thanh toán."
            );

            return "redirect:/cart";
        }

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem item : cartItems) {

            BigDecimal price = item.getProductVariant()
                    .getProduct()
                    .getBasePrice();

            BigDecimal itemTotal = price.multiply(
                    BigDecimal.valueOf(item.getQuantity())
            );

            totalAmount = totalAmount.add(itemTotal);
        }

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("totalAmount", totalAmount);

        if (!model.containsAttribute("checkoutForm")) {
            model.addAttribute(
                    "checkoutForm",
                    new CheckoutForm()
            );
        }

        return "checkout/index";
    }

    @PostMapping
    public String placeOrder(
            Principal principal,
            @Valid @ModelAttribute("checkoutForm")
            CheckoutForm checkoutForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {

            List<CartItem> cartItems =
                    cartService.getCartItems(principal.getName());

            BigDecimal totalAmount = calculateTotal(cartItems);

            model.addAttribute("cartItems", cartItems);
            model.addAttribute("totalAmount", totalAmount);

            return "checkout/index";
        }

        try {

            ShopOrder order = orderService.checkout(
                    principal.getName(),
                    checkoutForm
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đặt hàng thành công! Mã đơn hàng: "
                            + order.getOrderCode()
            );

            return "redirect:/account/orders/"
                    + order.getOrderId();

        } catch (AccessDeniedException e) {
            throw e;
        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );

            redirectAttributes.addFlashAttribute(
                    "checkoutForm",
                    checkoutForm
            );

            return "redirect:/checkout";

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Đặt hàng thất bại. Vui lòng thử lại."
            );

            redirectAttributes.addFlashAttribute(
                    "checkoutForm",
                    checkoutForm
            );

            return "redirect:/checkout";
        }
    }

    private BigDecimal calculateTotal(List<CartItem> cartItems) {

        BigDecimal total = BigDecimal.ZERO;

        for (CartItem item : cartItems) {

            BigDecimal price = item.getProductVariant()
                    .getProduct()
                    .getBasePrice();

            BigDecimal itemTotal = price.multiply(
                    BigDecimal.valueOf(item.getQuantity())
            );

            total = total.add(itemTotal);
        }

        return total;
    }
}
