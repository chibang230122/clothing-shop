package com.example.clothingshop.service;

import com.example.clothingshop.dto.ImportReceiptForm;
import com.example.clothingshop.entity.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
public class ImportReceiptService {
    @PersistenceContext private EntityManager em;
    @Transactional
    public ImportReceipt create(String email, ImportReceiptForm form) {
        User user = em.createQuery("select u from User u where lower(u.email)=lower(:email)", User.class).setParameter("email", email).getSingleResult();
        if (form.getLines()==null || form.getLines().isEmpty()) throw new IllegalArgumentException("Phiếu nhập chưa có sản phẩm.");
        ImportReceipt receipt = new ImportReceipt(); receipt.setImportDate(java.time.LocalDateTime.now()); receipt.setCreatedBy(user); receipt.setNote(form.getNote()); em.persist(receipt); em.flush();
        BigDecimal total=BigDecimal.ZERO;
        for (ImportReceiptForm.Line line : form.getLines()) {
            if (line.getVariantId()==null || line.getQuantity()==null || line.getQuantity()<=0 || line.getImportPrice()==null || line.getImportPrice().signum()<0) throw new IllegalArgumentException("Thông tin sản phẩm nhập không hợp lệ.");
            ProductVariant variant=em.find(ProductVariant.class,line.getVariantId(),LockModeType.PESSIMISTIC_WRITE);
            if (variant==null || !Boolean.TRUE.equals(variant.getIsActive())) throw new IllegalArgumentException("Không tìm thấy biến thể sản phẩm đang kinh doanh.");
            int before=variant.getStockQuantity(); int after=Math.addExact(before,line.getQuantity()); variant.setStockQuantity(after);
            BigDecimal subtotal=line.getImportPrice().multiply(BigDecimal.valueOf(line.getQuantity())); total=total.add(subtotal);
            ImportReceiptDetail detail=new ImportReceiptDetail(); detail.setProductVariant(variant); detail.setQuantity(line.getQuantity()); detail.setImportPrice(line.getImportPrice()); detail.setSubtotal(subtotal); receipt.addDetail(detail);
            em.persist(new StockTransaction(variant,"IMPORT",line.getQuantity(),before,after,"IMP-" + receipt.getImportReceiptId(), receipt.getNote(),user));
        }
        receipt.setTotalAmount(total); em.flush();
        return receipt;
    }
}
