
package com.example.clothingshop.repository;

import com.example.clothingshop.entity.ImportReceiptDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportReceiptDetailRepository
        extends JpaRepository<ImportReceiptDetail, Integer> {

    List<ImportReceiptDetail> findByImportReceipt_ImportReceiptId(
            Integer importReceiptId
    );
}
