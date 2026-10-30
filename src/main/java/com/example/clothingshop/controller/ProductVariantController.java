package com.example.clothingshop.controller;

import com.example.clothingshop.entity.Product;
import com.example.clothingshop.entity.ProductVariant;
import com.example.clothingshop.repository.ProductRepository;
import com.example.clothingshop.repository.ProductVariantRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/products/{productId}/variants")
public class ProductVariantController {
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;

    public ProductVariantController(ProductRepository productRepository,
                                    ProductVariantRepository variantRepository) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
    }

    // Danh sách biến thể
    @GetMapping
    public String list(@PathVariable Integer productId,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        Product product = productRepository.findById(productId).orElse(null);

        if (product == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy sản phẩm!");
            return "redirect:/admin/products";
        }

        model.addAttribute("product", product);
        model.addAttribute("variants",
                variantRepository.findByProduct_ProductId(productId));

        return "admin/variants";
    }

    // Hiển thị form thêm biến thể
    @GetMapping("/add")
    public String showAddForm(@PathVariable Integer productId,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        Product product = productRepository.findById(productId).orElse(null);

        if (product == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy sản phẩm!");
            return "redirect:/admin/products";
        }

        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setStockQuantity(0);
        variant.setIsActive(true);

        model.addAttribute("product", product);
        model.addAttribute("variant", variant);
        model.addAttribute("isEdit", false);

        return "admin/variant-form";
    }

    // Lưu biến thể mới
    @PostMapping("/save")
    public String save(@PathVariable Integer productId,
                       @ModelAttribute ProductVariant variant,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        Product product = productRepository.findById(productId).orElse(null);

        if (product == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy sản phẩm!");
            return "redirect:/admin/products";
        }

        String error = validateVariant(productId, variant, null);

        if (error != null) {
            model.addAttribute("product", product);
            model.addAttribute("variant", variant);
            model.addAttribute("isEdit", false);
            model.addAttribute("error", error);
            return "admin/variant-form";
        }

        variant.setProduct(product);
        variant.setSku(variant.getSku().trim());
        variant.setSize(variant.getSize().trim());
        variant.setColor(variant.getColor().trim());

        if (variant.getIsActive() == null) {
            variant.setIsActive(true);
        }

        variantRepository.save(variant);

        redirectAttributes.addFlashAttribute("success", "Thêm biến thể thành công!");
        return "redirect:/admin/products/" + productId + "/variants";
    }

    // Hiển thị form sửa biến thể
    @GetMapping("/edit/{variantId}")
    public String showEditForm(@PathVariable Integer productId,
                               @PathVariable Integer variantId,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        Product product = productRepository.findById(productId).orElse(null);
        ProductVariant variant = variantRepository.findById(variantId).orElse(null);

        if (product == null || variant == null
                || variant.getProduct() == null
                || !variant.getProduct().getProductId().equals(productId)) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy biến thể!");
            return "redirect:/admin/products/" + productId + "/variants";
        }

        model.addAttribute("product", product);
        model.addAttribute("variant", variant);
        model.addAttribute("isEdit", true);

        return "admin/variant-form";
    }

    // Cập nhật biến thể
    @PostMapping("/update")
    public String update(@PathVariable Integer productId,
                         @ModelAttribute ProductVariant formVariant,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Product product = productRepository.findById(productId).orElse(null);
        ProductVariant oldVariant = formVariant.getVariantId() == null
                ? null
                : variantRepository.findById(formVariant.getVariantId()).orElse(null);

        if (product == null || oldVariant == null
                || oldVariant.getProduct() == null
                || !oldVariant.getProduct().getProductId().equals(productId)) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy biến thể!");
            return "redirect:/admin/products/" + productId + "/variants";
        }

        String error = validateVariant(productId, formVariant, oldVariant.getVariantId());

        if (error != null) {
            model.addAttribute("product", product);
            model.addAttribute("variant", formVariant);
            model.addAttribute("isEdit", true);
            model.addAttribute("error", error);
            return "admin/variant-form";
        }

        oldVariant.setSku(formVariant.getSku().trim());
        oldVariant.setSize(formVariant.getSize().trim());
        oldVariant.setColor(formVariant.getColor().trim());
        oldVariant.setStockQuantity(formVariant.getStockQuantity());
        oldVariant.setIsActive(formVariant.getIsActive() != null
                && formVariant.getIsActive());

        variantRepository.save(oldVariant);

        redirectAttributes.addFlashAttribute("success", "Cập nhật biến thể thành công!");
        return "redirect:/admin/products/" + productId + "/variants";
    }

    // Kiểm tra dữ liệu thêm và sửa
    private String validateVariant(Integer productId,
                                   ProductVariant variant,
                                   Integer currentVariantId) {
        String sku = variant.getSku();
        String size = variant.getSize();
        String color = variant.getColor();

        if (sku == null || sku.trim().isEmpty()) {
            return "SKU không được để trống!";
        }

        sku = sku.trim();

        ProductVariant sameSku = variantRepository.findBySkuIgnoreCase(sku).orElse(null);

        if (sameSku != null
                && !sameSku.getVariantId().equals(currentVariantId)) {
            return "SKU đã tồn tại!";
        }

        if (size == null || size.trim().isEmpty()) {
            return "Size không được để trống!";
        }

        if (color == null || color.trim().isEmpty()) {
            return "Màu sắc không được để trống!";
        }

        size = size.trim();
        color = color.trim();

        boolean duplicateSizeColor =
                variantRepository.existsByProduct_ProductIdAndSizeIgnoreCaseAndColorIgnoreCase(
                        productId, size, color);

        if (duplicateSizeColor) {
            ProductVariant sameVariant =
                    variantRepository.findByProduct_ProductIdAndSizeIgnoreCaseAndColorIgnoreCase(
                            productId, size, color).orElse(null);

            if (sameVariant != null
                    && !sameVariant.getVariantId().equals(currentVariantId)) {
                return "Size và màu sắc này đã tồn tại trong sản phẩm!";
            }
        }

        if (variant.getStockQuantity() == null
                || variant.getStockQuantity() < 0) {
            return "Tồn kho không được để trống hoặc âm!";
        }

        return null;
    }

    // Ngừng kinh doanh biến thể, không xóa cứng
    @PostMapping("/deactivate/{variantId}")
    public String deactivate(@PathVariable Integer productId,
                             @PathVariable Integer variantId,
                             RedirectAttributes redirectAttributes) {
        ProductVariant variant = variantRepository.findById(variantId).orElse(null);

        if (variant == null
                || variant.getProduct() == null
                || !variant.getProduct().getProductId().equals(productId)) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy biến thể!");
            return "redirect:/admin/products/" + productId + "/variants";
        }

        variant.setIsActive(false);
        variantRepository.save(variant);

        redirectAttributes.addFlashAttribute("success", "Đã ngừng kinh doanh biến thể!");
        return "redirect:/admin/products/" + productId + "/variants";
    }
}