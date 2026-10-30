package com.example.clothingshop.controller;

import com.example.clothingshop.entity.Category;
import com.example.clothingshop.repository.CategoryRepository;
import com.example.clothingshop.repository.ProductRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryController(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    // Hiển thị danh sách danh mục
    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/categories";
    }

    // Hiển thị form thêm danh mục
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("category", new Category());
        return "admin/category-form";
    }

    // Xử lý thêm danh mục
    @PostMapping("/save")
    public String save(@ModelAttribute Category category, RedirectAttributes redirectAttributes) {
        String name = category.getCategoryName();

        if (name == null || name.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Tên danh mục không được để trống!");
            return "redirect:/admin/categories/add";
        }

        category.setCategoryName(name.trim());

        if (categoryRepository.existsByCategoryName(category.getCategoryName())) {
            redirectAttributes.addFlashAttribute("error", "Tên danh mục đã tồn tại!");
            return "redirect:/admin/categories/add";
        }

        if (category.getIsActive() == null) {
            category.setIsActive(true);
        }

        categoryRepository.save(category);

        redirectAttributes.addFlashAttribute("success", "Thêm danh mục thành công!");
        return "redirect:/admin/categories";
    }

    // Hiển thị form sửa danh mục
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        Category category = categoryRepository.findById(id).orElse(null);

        if (category == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy danh mục!");
            return "redirect:/admin/categories";
        }

        model.addAttribute("category", category);
        return "admin/category-edit";
    }

    // Xử lý cập nhật danh mục
    @PostMapping("/update")
    public String update(@ModelAttribute Category category, RedirectAttributes redirectAttributes) {

        Category oldCategory = categoryRepository.findById(category.getCategoryId()).orElse(null);

        if (oldCategory == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy danh mục!");
            return "redirect:/admin/categories";
        }

        String name = category.getCategoryName();

        if (name == null || name.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Tên danh mục không được để trống!");
            return "redirect:/admin/categories/edit/" + category.getCategoryId();
        }

        name = name.trim();

        if (categoryRepository.existsByCategoryName(name)
                && !name.equals(oldCategory.getCategoryName())) {
            redirectAttributes.addFlashAttribute("error", "Tên danh mục đã tồn tại!");
            return "redirect:/admin/categories/edit/" + category.getCategoryId();
        }

        oldCategory.setCategoryName(name);
        oldCategory.setDescription(category.getDescription());
        oldCategory.setIsActive(category.getIsActive() != null && category.getIsActive());

        categoryRepository.save(oldCategory);

        redirectAttributes.addFlashAttribute("success", "Cập nhật danh mục thành công!");
        return "redirect:/admin/categories";
    }

    // Xóa danh mục
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Integer id, RedirectAttributes redirectAttributes) {

        Category category = categoryRepository.findById(id).orElse(null);

        if (category == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy danh mục!");
            return "redirect:/admin/categories";
        }

        if (productRepository.existsByCategory_CategoryId(id)) {
            redirectAttributes.addFlashAttribute(
                    "error", "Không thể xóa danh mục đang có sản phẩm!"
            );
            return "redirect:/admin/categories";
        }

        categoryRepository.delete(category);

        redirectAttributes.addFlashAttribute("success", "Xóa danh mục thành công!");
        return "redirect:/admin/categories";
    }
}