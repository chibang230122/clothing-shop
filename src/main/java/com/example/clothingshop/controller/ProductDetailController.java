package com.example.clothingshop.controller;

import com.example.clothingshop.entity.Product;
import com.example.clothingshop.repository.ProductRepository;
import com.example.clothingshop.repository.ProductVariantRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

@Controller
@RequestMapping("/products")
public class ProductDetailController {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    public ProductDetailController(
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model) {

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

        return "product-detail";
    }
}