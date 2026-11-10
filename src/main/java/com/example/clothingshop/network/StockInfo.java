package com.example.clothingshop.network;

/** Thông tin tồn kho của một biến thể (size/màu) trả về cho client. */
public record StockInfo(String sku,
                        String productName,
                        String size,
                        String color,
                        int stockQuantity,
                        boolean active) {

    /** Còn bán được khi biến thể đang kinh doanh và tồn kho lớn hơn 0. */
    public boolean available() {
        return active && stockQuantity > 0;
    }
}
