package com.example.clothingshop.controller;

import com.example.clothingshop.entity.Voucher;
import com.example.clothingshop.repository.VoucherRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin/vouchers")
public class VoucherController {

    private final VoucherRepository voucherRepository;

    public VoucherController(VoucherRepository voucherRepository) {
        this.voucherRepository = voucherRepository;
    }

    @GetMapping
    public String adminVouchers(Model model) {
        model.addAttribute("vouchers", voucherRepository.findAll());
        model.addAttribute("newVoucher", new Voucher());
        return "admin/vouchers";
    }

    @PostMapping("/save")
    public String saveVoucher(@ModelAttribute("newVoucher") Voucher v, RedirectAttributes ra) {
        if (v.getCode() == null || v.getCode().trim().isEmpty()) {
            ra.addFlashAttribute("error", "Mã voucher không được để trống!");
            return "redirect:/admin/vouchers";
        }
        String cleanCode = v.getCode().trim().toUpperCase();
        if (voucherRepository.existsByCodeIgnoreCase(cleanCode)) {
            ra.addFlashAttribute("error", "Mã voucher '" + cleanCode + "' đã tồn tại!");
            return "redirect:/admin/vouchers";
        }
        if (v.getDiscountValue() == null || v.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            ra.addFlashAttribute("error", "Giá trị giảm phải lớn hơn 0!");
            return "redirect:/admin/vouchers";
        }
        if (v.getQuantity() == null || v.getQuantity() < 0) {
            ra.addFlashAttribute("error", "Số lượng voucher không được âm!");
            return "redirect:/admin/vouchers";
        }
        if (v.getStartDate() == null || v.getEndDate() == null || v.getEndDate().isBefore(v.getStartDate())) {
            ra.addFlashAttribute("error", "Ngày kết thúc phải sau ngày bắt đầu!");
            return "redirect:/admin/vouchers";
        }
        if (v.getMinOrderAmount() == null) {
            v.setMinOrderAmount(BigDecimal.ZERO);
        }

        v.setCode(cleanCode);
        v.setIsActive(true);
        voucherRepository.save(v);
        ra.addFlashAttribute("success", "Thêm voucher " + cleanCode + " thành công!");
        return "redirect:/admin/vouchers";
    }

    @PostMapping("/toggle/{id}")
    public String toggleVoucher(@PathVariable Integer id, RedirectAttributes ra) {
        voucherRepository.findById(id).ifPresent(v -> {
            v.setIsActive(!Boolean.TRUE.equals(v.getIsActive()));
            voucherRepository.save(v);
            ra.addFlashAttribute("success", "Đã cập nhật trạng thái voucher " + v.getCode() + "!");
        });
        return "redirect:/admin/vouchers";
    }
}