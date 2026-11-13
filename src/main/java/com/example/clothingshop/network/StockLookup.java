package com.example.clothingshop.network;

import java.util.Optional;

/** Tra cứu tồn kho theo SKU. Lớp thực thi (InventoryService) đọc từ SQL Server. */
public interface StockLookup {
    Optional<StockInfo> findStockBySku(String sku);
}
