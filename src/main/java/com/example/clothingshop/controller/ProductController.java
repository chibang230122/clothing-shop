package com.example.clothingshop.controller;

import org.springframework.web.bind.annotation.RequestParam;
import com.example.clothingshop.entity.Product;
import com.example.clothingshop.repository.CategoryRepository;
import com.example.clothingshop.repository.ProductRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin/products")
public class ProductController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductController(ProductRepository productRepository,
                             CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    // Hiển thị danh sách sản phẩm
    @GetMapping
    public String list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            Model model) {

        if (keyword != null && keyword.trim().isEmpty()) {
            keyword = null;
        }

        model.addAttribute("products", productRepository.searchProducts(keyword, categoryId, minPrice, maxPrice));
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);

        return "admin/products";
    }

    // Hiển thị form thêm sản phẩm
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/product-form";
    }

    // Thêm sản phẩm
    @PostMapping("/save")
    public String save(@ModelAttribute Product product,
                       RedirectAttributes redirectAttributes,
                       Model model) {

        String name = product.getProductName();

        // Kiểm tra tên trống
        if (name == null || name.trim().isEmpty()) {
            model.addAttribute("error", "Tên sản phẩm không được để trống!");
            model.addAttribute("categories", categoryRepository.findAll());
            return "admin/product-form";
        }

        name = name.trim();

        // Kiểm tra trùng tên
        if (productRepository.existsByProductNameIgnoreCase(name)) {
            model.addAttribute("error", "Tên sản phẩm đã tồn tại!");
            model.addAttribute("categories", categoryRepository.findAll());
            return "admin/product-form";
        }

        // Kiểm tra giá
        if (product.getBasePrice() == null
                || product.getBasePrice().compareTo(BigDecimal.ZERO) <= 0) {
            model.addAttribute("error", "Giá sản phẩm phải lớn hơn 0!");
            model.addAttribute("categories", categoryRepository.findAll());
            return "admin/product-form";
        }

        // Kiểm tra danh mục
        if (product.getCategory() == null
                || product.getCategory().getCategoryId() == null
                || !categoryRepository.existsById(
                product.getCategory().getCategoryId())) {
            model.addAttribute("error", "Vui lòng chọn danh mục hợp lệ!");
            model.addAttribute("categories", categoryRepository.findAll());
            return "admin/product-form";
        }

        product.setProductName(name);

        if (product.getIsActive() == null) {
            product.setIsActive(true);
        }

        product.setCreatedAt(LocalDateTime.now());
        productRepository.save(product);

        redirectAttributes.addFlashAttribute(
                "success", "Thêm sản phẩm thành công!");

        return "redirect:/admin/products";
    }

    // Hiển thị form sửa sản phẩm
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id,
                               Model model,
                               RedirectAttributes redirectAttributes) {

        Product product = productRepository.findById(id).orElse(null);

        if (product == null) {
            redirectAttributes.addFlashAttribute(
                    "error", "Không tìm thấy sản phẩm!");
            return "redirect:/admin/products";
        }

        model.addAttribute("product", product);
        model.addAttribute("categories", categoryRepository.findAll());

        return "admin/product-edit";
    }

    // Cập nhật sản phẩm
    @PostMapping("/update")
    public String update(@ModelAttribute Product product,
                         RedirectAttributes redirectAttributes,
                         Model model) {

        Product oldProduct = productRepository
                .findById(product.getProductId()).orElse(null);

        if (oldProduct == null) {
            redirectAttributes.addFlashAttribute(
                    "error", "Không tìm thấy sản phẩm!");
            return "redirect:/admin/products";
        }

        String name = product.getProductName();

        // Kiểm tra tên trống
        if (name == null || name.trim().isEmpty()) {
            model.addAttribute("error", "Tên sản phẩm không được để trống!");
            model.addAttribute("categories", categoryRepository.findAll());
            model.addAttribute("product", product);
            return "admin/product-edit";
        }

        name = name.trim();

        // Kiểm tra trùng tên nhưng vẫn cho phép giữ tên cũ
        if (productRepository.existsByProductNameIgnoreCase(name)
                && !name.equalsIgnoreCase(oldProduct.getProductName())) {
            model.addAttribute("error", "Tên sản phẩm đã tồn tại!");
            model.addAttribute("categories", categoryRepository.findAll());
            model.addAttribute("product", product);
            return "admin/product-edit";
        }

        // Kiểm tra giá
        if (product.getBasePrice() == null
                || product.getBasePrice().compareTo(BigDecimal.ZERO) <= 0) {
            model.addAttribute("error", "Giá sản phẩm không được để trống hoặc âm!");
            model.addAttribute("categories", categoryRepository.findAll());
            model.addAttribute("product", product);
            return "admin/product-edit";
        }

        // Kiểm tra danh mục
        if (product.getCategory() == null
                || product.getCategory().getCategoryId() == null
                || !categoryRepository.existsById(
                product.getCategory().getCategoryId())) {
            model.addAttribute("error", "Vui lòng chọn danh mục hợp lệ!");
            model.addAttribute("categories", categoryRepository.findAll());
            model.addAttribute("product", product);
            return "admin/product-edit";
        }

        // Cập nhật thông tin
        oldProduct.setProductName(name);
        oldProduct.setCategory(product.getCategory());
        oldProduct.setDescription(product.getDescription());
        oldProduct.setBasePrice(product.getBasePrice());
        oldProduct.setImageUrl(product.getImageUrl());
        oldProduct.setIsActive(
                product.getIsActive() != null && product.getIsActive());

        productRepository.save(oldProduct);

        redirectAttributes.addFlashAttribute(
                "success", "Cập nhật sản phẩm thành công!");

        return "redirect:/admin/products";
    }
    // Ngừng kinh doanh sản phẩm
    @PostMapping("/deactivate/{id}")
    public String deactivate(@PathVariable Integer id,
                             RedirectAttributes redirectAttributes) {
        Product product = productRepository.findById(id).orElse(null);

        if (product == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy sản phẩm!");
            return "redirect:/admin/products";
        }

        product.setIsActive(false);
        productRepository.save(product);

        redirectAttributes.addFlashAttribute("success", "Đã ngừng kinh doanh sản phẩm!");
        return "redirect:/admin/products";
    }
}