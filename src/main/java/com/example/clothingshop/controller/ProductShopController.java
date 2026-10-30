package com.example.clothingshop.controller;

import com.example.clothingshop.entity.Product;
import com.example.clothingshop.repository.CategoryRepository;
import com.example.clothingshop.repository.ProductRepository;
import com.example.clothingshop.repository.ProductVariantRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Controller
public class ProductShopController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository productVariantRepository;

    private static final int PAGE_SIZE = 8;

    public ProductShopController(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ProductVariantRepository productVariantRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productVariantRepository = productVariantRepository;
    }

    @GetMapping("/products")
    public String products(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String color,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "newest") String sort,
            Model model) {

        keyword = clean(keyword);
        sku = clean(sku);
        size = clean(size);
        color = clean(color);

        // Lấy sản phẩm theo tên, danh mục và giá
        List<Product> products = productRepository.searchProducts(
                keyword, categoryId, minPrice, maxPrice);

        // Lọc thêm theo SKU, size và màu sắc
        if (sku != null || size != null || color != null) {
            List<Integer> ids =
                    productVariantRepository.findProductIdsByVariantFilters(
                            sku, size, color);

            products.removeIf(product ->
                    !ids.contains(product.getProductId()));
        }

        // Sắp xếp sản phẩm
        switch (sort) {
            case "priceAsc":
                products.sort(Comparator.comparing(Product::getBasePrice));
                break;

            case "priceDesc":
                products.sort(Comparator.comparing(Product::getBasePrice).reversed());
                break;

            case "newest":
            default:
                sort = "newest";
                products.sort(Comparator.comparing(
                        Product::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())));
                break;
        }

        // Tính số trang
        int totalProducts = products.size();
        int totalPages = (int) Math.ceil((double) totalProducts / PAGE_SIZE);

        if (page < 0) {
            page = 0;
        }

        if (totalPages > 0 && page >= totalPages) {
            page = totalPages - 1;
        }

        // Lấy sản phẩm của trang hiện tại
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, totalProducts);

        List<Product> pageProducts =
                products.subList(start, end);

        // Truyền dữ liệu sang giao diện
        model.addAttribute("products", pageProducts);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("sizes", productVariantRepository.findAllActiveSizes());
        model.addAttribute("colors", productVariantRepository.findAllActiveColors());

        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("sku", sku);
        model.addAttribute("size", size);
        model.addAttribute("color", color);

        model.addAttribute("page", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("sort", sort);

        return "products";
    }

    private String clean(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}