
package com.example.clothingshop.repository;

import com.example.clothingshop.entity.ImportReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImportReceiptRepository
        extends JpaRepository<ImportReceipt, Integer> {
}
