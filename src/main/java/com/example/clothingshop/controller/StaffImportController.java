package com.example.clothingshop.controller;

import com.example.clothingshop.dto.ImportReceiptForm;
import com.example.clothingshop.entity.ProductVariant;
import com.example.clothingshop.repository.ProductVariantRepository;
import com.example.clothingshop.repository.ImportReceiptRepository;
import com.example.clothingshop.service.ImportReceiptService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;

@Controller
@RequestMapping("/staff/imports")
public class StaffImportController {
    private final ProductVariantRepository variants;
    private final ImportReceiptService service;
    private final ImportReceiptRepository receipts;
    public StaffImportController(ProductVariantRepository variants, ImportReceiptService service, ImportReceiptRepository receipts){this.variants=variants;this.service=service;this.receipts=receipts;}
    @GetMapping public String list(Model model){model.addAttribute("receipts",receipts.findAll(Sort.by(Sort.Direction.DESC,"importDate")));return "staff/imports/list";}
    @GetMapping("/new") public String createForm(Model model){model.addAttribute("form",new ImportReceiptForm()); model.addAttribute("variants",variants.findAll(Sort.by("product.productName").ascending())); return "staff/imports/form";}
    @PostMapping public String create(@ModelAttribute("form") ImportReceiptForm form, Principal principal, RedirectAttributes ra, Model model){
        try {var receipt=service.create(principal.getName(),form); ra.addFlashAttribute("successMessage","Tạo phiếu nhập #"+receipt.getImportReceiptId()+" thành công."); return "redirect:/staff/imports/new";}
        catch(Exception e){model.addAttribute("errorMessage",e.getMessage()); model.addAttribute("variants",variants.findAll(Sort.by("product.productName").ascending())); return "staff/imports/form";}
    }
}
