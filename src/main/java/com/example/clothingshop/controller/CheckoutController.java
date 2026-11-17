
package com.example.clothingshop.controller;

import com.example.clothingshop.dto.CheckoutForm;
import com.example.clothingshop.entity.CartItem;
import com.example.clothingshop.entity.ShopOrder;
import com.example.clothingshop.service.CartService;
import com.example.clothingshop.service.OrderService;
import com.example.clothingshop.repository.ProductVariantRepository;
import com.example.clothingshop.entity.ProductVariant;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    private final CartService cartService;
    private final OrderService orderService;
    private final ProductVariantRepository productVariantRepository;

    public CheckoutController(
            CartService cartService,
            OrderService orderService,
            ProductVariantRepository productVariantRepository) {

        this.cartService = cartService;
        this.orderService = orderService;
        this.productVariantRepository = productVariantRepository;
    }

    @GetMapping("/buy-now/{variantId}")
    public String showBuyNow(
            @PathVariable Integer variantId,
            @RequestParam(defaultValue = "1") Integer quantity,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (quantity == null || quantity < 1 || quantity > 99) {
            redirectAttributes.addFlashAttribute("errorMessage", "Số lượng mua không hợp lệ.");
            return "redirect:/products";
        }
        ProductVariant variant = productVariantRepository.findById(variantId).orElse(null);
        if (variant == null || !Boolean.TRUE.equals(variant.getIsActive())
                || !Boolean.TRUE.equals(variant.getProduct().getIsActive())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Sản phẩm không còn kinh doanh.");
            return "redirect:/products";
        }
        if (variant.getStockQuantity() == null || variant.getStockQuantity() < quantity) {
            redirectAttributes.addFlashAttribute("errorMessage", "Sản phẩm không đủ tồn kho.");
            return "redirect:/products/" + variant.getProduct().getProductId();
        }
        model.addAttribute("variant", variant);
        model.addAttribute("quantity", quantity);
        model.addAttribute("totalAmount", variant.getProduct().getBasePrice()
                .multiply(BigDecimal.valueOf(quantity)));
        if (!model.containsAttribute("checkoutForm")) {
            model.addAttribute("checkoutForm", new CheckoutForm());
        }
        return "checkout/buy-now";
    }

    @PostMapping("/buy-now/{variantId}")
    public String placeBuyNowOrder(
            @PathVariable Integer variantId,
            @RequestParam Integer quantity,
            Principal principal,
            @Valid @ModelAttribute("checkoutForm") CheckoutForm checkoutForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            ProductVariant variant = productVariantRepository.findById(variantId).orElse(null);
            if (variant == null) return "redirect:/products";
            model.addAttribute("variant", variant);
            model.addAttribute("quantity", quantity);
            model.addAttribute("totalAmount", variant.getProduct().getBasePrice()
                    .multiply(BigDecimal.valueOf(Math.max(1, quantity))));
            return "checkout/buy-now";
        }
        try {
            ShopOrder order = orderService.checkoutNow(
                    principal.getName(), variantId, quantity, checkoutForm);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Mua ngay thành công! Mã đơn hàng: " + order.getOrderCode());
            return "redirect:/account/orders/" + order.getOrderId();
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("checkoutForm", checkoutForm);
            return "redirect:/checkout/buy-now/" + variantId + "?quantity=" + quantity;
        }
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
