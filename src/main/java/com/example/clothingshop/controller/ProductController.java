package com.example.clothingshop.controller;

import com.example.clothingshop.entity.Product;
import com.example.clothingshop.repository.CategoryRepository;
import com.example.clothingshop.repository.ProductRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/product-form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Product product,
                       @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                       RedirectAttributes redirectAttributes,
                       Model model) {

        String name = product.getProductName();

        if (name == null || name.trim().isEmpty()) {
            model.addAttribute("error", "Tên sản phẩm không được để trống!");
            model.addAttribute("categories", categoryRepository.findAll());
            return "admin/product-form";
        }

        name = name.trim();

        if (productRepository.existsByProductNameIgnoreCase(name)) {
            model.addAttribute("error", "Tên sản phẩm đã tồn tại!");
            model.addAttribute("categories", categoryRepository.findAll());
            return "admin/product-form";
        }

        if (product.getBasePrice() == null
                || product.getBasePrice().compareTo(BigDecimal.ZERO) <= 0) {
            model.addAttribute("error", "Giá sản phẩm phải lớn hơn 0!");
            model.addAttribute("categories", categoryRepository.findAll());
            return "admin/product-form";
        }

        if (product.getCategory() == null
                || product.getCategory().getCategoryId() == null
                || !categoryRepository.existsById(product.getCategory().getCategoryId())) {
            model.addAttribute("error", "Vui lòng chọn danh mục hợp lệ!");
            model.addAttribute("categories", categoryRepository.findAll());
            return "admin/product-form";
        }

        // Lưu file ảnh tải lên từ máy tính (nếu có)
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String savedUrl = saveProductImage(imageFile);
                product.setImageUrl(savedUrl);
            } catch (IllegalArgumentException | IOException e) {
                model.addAttribute("error", "Lỗi tải ảnh: " + e.getMessage());
                model.addAttribute("categories", categoryRepository.findAll());
                return "admin/product-form";
            }
        }

        product.setProductName(name);
        if (product.getIsActive() == null) {
            product.setIsActive(true);
        }

        product.setCreatedAt(LocalDateTime.now());
        productRepository.save(product);

        redirectAttributes.addFlashAttribute("success", "Thêm sản phẩm thành công!");
        return "redirect:/admin/products";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id,
                               Model model,
                               RedirectAttributes redirectAttributes) {

        Product product = productRepository.findById(id).orElse(null);

        if (product == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy sản phẩm!");
            return "redirect:/admin/products";
        }

        model.addAttribute("product", product);
        model.addAttribute("categories", categoryRepository.findAll());

        return "admin/product-edit";
    }

    @PostMapping("/update")
    public String update(@ModelAttribute Product product,
                         @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                         RedirectAttributes redirectAttributes,
                         Model model) {

        Product oldProduct = productRepository.findById(product.getProductId()).orElse(null);

        if (oldProduct == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy sản phẩm!");
            return "redirect:/admin/products";
        }

        String name = product.getProductName();

        if (name == null || name.trim().isEmpty()) {
            model.addAttribute("error", "Tên sản phẩm không được để trống!");
            model.addAttribute("categories", categoryRepository.findAll());
            model.addAttribute("product", product);
            return "admin/product-edit";
        }

        name = name.trim();

        if (productRepository.existsByProductNameIgnoreCase(name)
                && !name.equalsIgnoreCase(oldProduct.getProductName())) {
            model.addAttribute("error", "Tên sản phẩm đã tồn tại!");
            model.addAttribute("categories", categoryRepository.findAll());
            model.addAttribute("product", product);
            return "admin/product-edit";
        }

        if (product.getBasePrice() == null
                || product.getBasePrice().compareTo(BigDecimal.ZERO) <= 0) {
            model.addAttribute("error", "Giá sản phẩm không được để trống hoặc âm!");
            model.addAttribute("categories", categoryRepository.findAll());
            model.addAttribute("product", product);
            return "admin/product-edit";
        }

        if (product.getCategory() == null
                || product.getCategory().getCategoryId() == null
                || !categoryRepository.existsById(product.getCategory().getCategoryId())) {
            model.addAttribute("error", "Vui lòng chọn danh mục hợp lệ!");
            model.addAttribute("categories", categoryRepository.findAll());
            model.addAttribute("product", product);
            return "admin/product-edit";
        }

        // Nếu có chọn ảnh mới thì cập nhật ảnh mới, nếu không thì giữ nguyên ảnh cũ
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String savedUrl = saveProductImage(imageFile);
                oldProduct.setImageUrl(savedUrl);
            } catch (IllegalArgumentException | IOException e) {
                model.addAttribute("error", "Lỗi tải ảnh: " + e.getMessage());
                model.addAttribute("categories", categoryRepository.findAll());
                model.addAttribute("product", product);
                return "admin/product-edit";
            }
        }

        oldProduct.setProductName(name);
        oldProduct.setCategory(product.getCategory());
        oldProduct.setDescription(product.getDescription());
        oldProduct.setBasePrice(product.getBasePrice());
        oldProduct.setIsActive(product.getIsActive() != null && product.getIsActive());

        productRepository.save(oldProduct);

        redirectAttributes.addFlashAttribute("success", "Cập nhật sản phẩm thành công!");
        return "redirect:/admin/products";
    }

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

    // Hàm hỗ trợ lưu file ảnh vào static/images và target để hiển thị ngay
    private String saveProductImage(MultipartFile imageFile) throws IOException {
        String contentType = imageFile.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Chỉ chấp nhận tệp hình ảnh (JPG, PNG, WEBP...)");
        }

        String originalName = imageFile.getOriginalFilename() != null
                ? imageFile.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
                : "product.jpg";
        String fileName = System.currentTimeMillis() + "_" + originalName;

        Path srcDir = Paths.get("src/main/resources/static/images");
        Files.createDirectories(srcDir);
        Path srcFile = srcDir.resolve(fileName);
        Files.copy(imageFile.getInputStream(), srcFile, StandardCopyOption.REPLACE_EXISTING);

        Path targetDir = Paths.get("target/classes/static/images");
        if (Files.exists(targetDir)) {
            Files.copy(srcFile, targetDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        }

        return "/images/" + fileName;
    }
}