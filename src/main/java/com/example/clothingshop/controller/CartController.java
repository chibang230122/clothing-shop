
package com.example.clothingshop.controller;

import com.example.clothingshop.entity.CartItem;
import com.example.clothingshop.service.CartService;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public String viewCart(
            Principal principal,
            Model model) {

        List<CartItem> cartItems =
                cartService.getCartItems(principal.getName());

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem item : cartItems) {
            BigDecimal price = item.getProductVariant()
                    .getProduct()
                    .getBasePrice();

            BigDecimal itemTotal = price.multiply(
                    BigDecimal.valueOf(item.getQuantity()));

            totalAmount = totalAmount.add(itemTotal);
        }

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("totalItems", cartItems.size());

        return "cart/index";
    }

    @PostMapping("/add")
    public String addToCart(
            @RequestParam Integer variantId,
            @RequestParam(defaultValue = "1") Integer quantity,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {
            cartService.addItem(
                    principal.getName(),
                    variantId,
                    quantity
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã thêm sản phẩm vào giỏ hàng."
            );

        } catch (IllegalArgumentException ex) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );
        }

        return "redirect:/cart";
    }

    @PostMapping("/add-ajax")
    @ResponseBody
    public Map<String, Object> addToCartAjax(
            @RequestParam(required = false) Integer productId,
            @RequestParam(required = false) Integer variantId,
            @RequestParam(defaultValue = "1") Integer quantity,
            Principal principal) {

        try {
            if (productId != null) {
                cartService.addProductItem(principal.getName(), productId, quantity);
            } else {
                cartService.addItem(principal.getName(), variantId, quantity);
            }

            return Map.of(
                    "success", true,
                    "message", "Đã thêm sản phẩm vào giỏ hàng.",
                    "count", getCartCount(principal.getName())
            );
        } catch (IllegalArgumentException ex) {
            return Map.of(
                    "success", false,
                    "message", ex.getMessage(),
                    "count", getCartCount(principal.getName())
            );
        }
    }

    @GetMapping("/count")
    @ResponseBody
    public Map<String, Integer> cartCount(Principal principal) {
        return Map.of("count", getCartCount(principal.getName()));
    }

    private int getCartCount(String email) {
        return cartService.getCartItems(email).stream()
                .mapToInt(item -> item.getQuantity() == null ? 0 : item.getQuantity())
                .sum();
    }

    @PostMapping("/update")
    public String updateCartItem(
            @RequestParam Integer variantId,
            @RequestParam Integer quantity,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {
            cartService.updateQuantity(
                    principal.getName(),
                    variantId,
                    quantity
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã cập nhật số lượng sản phẩm."
            );

        } catch (IllegalArgumentException ex) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );
        }

        return "redirect:/cart";
    }

    @PostMapping("/remove")
    public String removeCartItem(
            @RequestParam Integer variantId,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {
            cartService.removeItem(
                    principal.getName(),
                    variantId
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã xóa sản phẩm khỏi giỏ hàng."
            );

        } catch (IllegalArgumentException ex) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );
        }
        return "redirect:/cart";
    }
}
