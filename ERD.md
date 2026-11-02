
# ERD - ClothingShopDB

```mermaid
erDiagram
    Roles ||--o{ Users : has
    Users ||--o{ Orders : places
    Users ||--o| Carts : owns
    Categories ||--o{ Products : contains
    Products ||--o{ ProductVariants : has
    Orders ||--|{ OrderItems : includes
    ProductVariants ||--o{ OrderItems : references
    Carts ||--o{ CartItems : contains
    ProductVariants ||--o{ CartItems : selected_in
```

## Mô tả quan hệ

- Roles - Users: một vai trò có nhiều người dùng.
- Users - Orders: một người dùng có thể đặt nhiều đơn hàng.
- Users - Carts: mỗi người dùng có tối đa một giỏ hàng.
- Categories - Products: một danh mục có nhiều sản phẩm.
- Products - ProductVariants: một sản phẩm có nhiều phiên bản.
- Orders - OrderItems: một đơn hàng có nhiều dòng chi tiết.
- ProductVariants - OrderItems: một phiên bản có thể xuất hiện trong nhiều đơn hàng.
- Carts - CartItems: một giỏ hàng có nhiều dòng sản phẩm.
- ProductVariants - CartItems: một phiên bản có thể nằm trong nhiều giỏ hàng.

![ERD - ClothingShopDB](docs/images/Ecommerce%20Orders%20and%20Product%20Variants%20Data%20Model-2026-10-23-061937.png)
