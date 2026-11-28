package com.example.clothingshop.controller;

import com.example.clothingshop.entity.Product;
import com.example.clothingshop.entity.ProductReview;
import com.example.clothingshop.entity.User;
import com.example.clothingshop.repository.ProductRepository;
import com.example.clothingshop.repository.ProductReviewRepository;
import com.example.clothingshop.repository.ProductVariantRepository;
import com.example.clothingshop.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/products")
public class ProductDetailController {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductReviewRepository productReviewRepository;
    private final UserRepository userRepository;

    public ProductDetailController(
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            ProductReviewRepository productReviewRepository,
            UserRepository userRepository) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.productReviewRepository = productReviewRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model, Principal principal) {

        Optional<Product> result = productRepository.findById(id);

        if (result.isEmpty() || !Boolean.TRUE.equals(result.get().getIsActive())) {
            return "redirect:/products";
        }

        Product product = result.get();

        model.addAttribute("product", product);
        model.addAttribute(
                "variants",
                productVariantRepository.findByProduct_ProductIdAndIsActiveTrue(id)
        );

        // Nạp danh sách đánh giá và tính điểm sao trung bình
        List<ProductReview> reviews = productReviewRepository.findByProduct_ProductIdOrderByCreatedAtDesc(id);
        double avgRating = reviews.stream()
                .mapToInt(ProductReview::getRating)
                .average()
                .orElse(0.0);

        boolean canReview = false;
        boolean alreadyReviewed = false;
        if (principal != null) {
            String email = principal.getName();
            boolean boughtCompleted = productReviewRepository.countCompletedPurchases(email, id) > 0;
            alreadyReviewed = productReviewRepository.existsByProduct_ProductIdAndUser_EmailIgnoreCase(id, email);
            canReview = boughtCompleted && !alreadyReviewed;
        }

        model.addAttribute("reviews", reviews);
        model.addAttribute("avgRating", String.format("%.1f", avgRating));
        model.addAttribute("canReview", canReview);
        model.addAttribute("alreadyReviewed", alreadyReviewed);

        // Danh sách 4 sản phẩm gợi ý
        List<Product> recommendedProducts = productRepository.findByIsActiveTrue()
                .stream()
                .filter(item -> !item.getProductId().equals(id))
                .limit(4)
                .toList();
        model.addAttribute("recommendedProducts", recommendedProducts);

        return "product-detail";
    }

    @PostMapping("/{id}/reviews")
    public String submitReview(@PathVariable Integer id,
                               @RequestParam Integer rating,
                               @RequestParam String comment,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }

        String email = principal.getName();

        if (productReviewRepository.countCompletedPurchases(email, id) == 0) {
            redirectAttributes.addFlashAttribute("reviewError",
                    "Bạn chỉ có thể đánh giá sau khi đã mua và hoàn tất đơn hàng sản phẩm này!");
            return "redirect:/products/" + id + "#reviews-section";
        }

        if (productReviewRepository.existsByProduct_ProductIdAndUser_EmailIgnoreCase(id, email)) {
            redirectAttributes.addFlashAttribute("reviewError",
                    "Bạn đã đánh giá sản phẩm này rồi!");
            return "redirect:/products/" + id + "#reviews-section";
        }

        Product product = productRepository.findById(id).orElse(null);
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);

        if (product != null && user != null) {
            ProductReview review = new ProductReview();
            review.setProduct(product);
            review.setUser(user);
            review.setRating(Math.max(1, Math.min(5, rating)));
            review.setComment(comment != null ? comment.trim() : "");
            review.setCreatedAt(LocalDateTime.now());
            productReviewRepository.save(review);

            redirectAttributes.addFlashAttribute("reviewSuccess",
                    "Cảm ơn bạn đã gửi đánh giá sản phẩm!");
        }

        return "redirect:/products/" + id + "#reviews-section";
    }
}