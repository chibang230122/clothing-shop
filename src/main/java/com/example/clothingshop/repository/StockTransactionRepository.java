package com.example.clothingshop.repository;

import com.example.clothingshop.entity.StockTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {
    @EntityGraph(attributePaths = {"productVariant", "productVariant.product", "createdBy"})
    @Query("select t from StockTransaction t where (:type is null or t.transactionType = :type) order by t.createdAt desc, t.transactionId desc")
    Page<StockTransaction> findHistory(@Param("type") String type, Pageable pageable);
}
