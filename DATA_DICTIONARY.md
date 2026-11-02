
# Data Dictionary - ClothingShopDB

## 1. Tổng quan

Database: `ClothingShopDB`

Mục đích: Quản lý người dùng, danh mục, sản phẩm, phiên bản sản phẩm,
đơn hàng và giỏ hàng cho website bán quần áo.

## 2. Danh sách bảng

| STT | Bảng | Mục đích |
|---|---|---|
| 1 | Roles | Lưu các vai trò người dùng |
| 2 | Users | Lưu thông tin tài khoản |
| 3 | Categories | Lưu danh mục sản phẩm |
| 4 | Products | Lưu thông tin sản phẩm |
| 5 | ProductVariants | Lưu size, màu sắc và tồn kho |
| 6 | Orders | Lưu thông tin đơn hàng |
| 7 | OrderItems | Lưu chi tiết sản phẩm trong đơn hàng |
| 8 | Carts | Lưu giỏ hàng của người dùng |
| 9 | CartItems | Lưu sản phẩm trong giỏ hàng |

## 3. Quy ước

- PK: Khóa chính (Primary Key).
- FK: Khóa ngoại (Foreign Key).
- UNIQUE: Giá trị không được trùng lặp.
- NOT NULL: Không được để trống.
- IDENTITY: SQL Server tự tăng giá trị.


## 4. Chi tiết cấu trúc các bảng

### 4.1. Bảng Roles

**Mục đích:** Lưu danh sách vai trò trong hệ thống.

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---|---|---|---|
| RoleId | INT | PK, IDENTITY | Mã vai trò |
| RoleName | NVARCHAR(30) | NOT NULL, UNIQUE | Tên vai trò |

### 4.2. Bảng Users

**Mục đích:** Lưu thông tin tài khoản người dùng.

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---|---|---|---|
| UserId | INT | PK, IDENTITY | Mã người dùng |
| RoleId | INT | FK, NOT NULL | Vai trò của người dùng |
| FullName | NVARCHAR(100) | NOT NULL | Họ và tên |
| Email | NVARCHAR(150) | NOT NULL, UNIQUE | Email đăng nhập |
| Phone | VARCHAR(20) | NULL | Số điện thoại |
| PasswordHash | NVARCHAR(255) | NOT NULL | Mật khẩu đã băm |
| Address | NVARCHAR(300) | NULL | Địa chỉ |
| IsActive | BIT | NOT NULL, DEFAULT 1 | Trạng thái hoạt động |
| CreatedAt | DATETIME2(0) | NOT NULL, DEFAULT SYSDATETIME() | Thời điểm tạo tài khoản |

### 4.3. Bảng Categories

**Mục đích:** Phân loại sản phẩm.

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---|---|---|---|
| CategoryId | INT | PK, IDENTITY | Mã danh mục |
| CategoryName | NVARCHAR(100) | NOT NULL, UNIQUE | Tên danh mục |
| Description | NVARCHAR(500) | NULL | Mô tả danh mục |
| IsActive | BIT | NOT NULL, DEFAULT 1 | Trạng thái hoạt động |

### 4.4. Bảng Products

**Mục đích:** Lưu thông tin chung của sản phẩm.

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---|---|---|---|
| ProductId | INT | PK, IDENTITY | Mã sản phẩm |
| CategoryId | INT | FK, NOT NULL | Danh mục sản phẩm |
| ProductName | NVARCHAR(200) | NOT NULL | Tên sản phẩm |
| Description | NVARCHAR(MAX) | NULL | Mô tả sản phẩm |
| BasePrice | DECIMAL(18,2) | NOT NULL, CHECK >= 0 | Giá cơ bản |
| ImageUrl | NVARCHAR(500) | NULL | Đường dẫn hình ảnh |
| IsActive | BIT | NOT NULL, DEFAULT 1 | Trạng thái sản phẩm |
| CreatedAt | DATETIME2(0) | NOT NULL, DEFAULT SYSDATETIME() | Thời điểm tạo sản phẩm |

### 4.5. Bảng ProductVariants

**Mục đích:** Quản lý từng phiên bản sản phẩm theo kích cỡ và màu sắc.

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---|---|---|---|
| VariantId | INT | PK, IDENTITY | Mã phiên bản |
| ProductId | INT | FK, NOT NULL | Sản phẩm tương ứng |
| SKU | VARCHAR(50) | NOT NULL, UNIQUE | Mã định danh phiên bản |
| Size | NVARCHAR(10) | NOT NULL | Kích cỡ |
| Color | NVARCHAR(40) | NOT NULL | Màu sắc |
| StockQuantity | INT | NOT NULL, DEFAULT 0, CHECK >= 0 | Số lượng tồn kho |
| IsActive | BIT | NOT NULL, DEFAULT 1 | Trạng thái phiên bản |

**Ràng buộc bổ sung:** UNIQUE(ProductId, Size, Color), không cho phép trùng tổ hợp sản phẩm, kích cỡ và màu sắc.

### 4.6. Bảng Orders

**Mục đích:** Lưu thông tin đặt hàng, giao hàng và thanh toán.

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---|---|---|---|
| OrderId | BIGINT | PK, IDENTITY | Mã đơn hàng |
| UserId | INT | FK, NOT NULL | Người đặt hàng |
| OrderCode | VARCHAR(30) | NOT NULL, UNIQUE | Mã đơn hàng hiển thị |
| OrderDate | DATETIME2(0) | NOT NULL, DEFAULT SYSDATETIME() | Ngày đặt hàng |
| RecipientName | NVARCHAR(100) | NOT NULL | Tên người nhận |
| RecipientPhone | VARCHAR(20) | NOT NULL | Điện thoại người nhận |
| ShippingAddress | NVARCHAR(300) | NOT NULL | Địa chỉ giao hàng |
| Subtotal | DECIMAL(18,2) | NOT NULL, CHECK >= 0 | Tiền hàng trước phí giao |
| ShippingFee | DECIMAL(18,2) | NOT NULL, DEFAULT 0, CHECK >= 0 | Phí giao hàng |
| TotalAmount | DECIMAL(18,2) | NOT NULL | Tổng tiền đơn hàng |
| OrderStatus | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | Trạng thái đơn hàng |
| PaymentMethod | VARCHAR(20) | NOT NULL, DEFAULT 'COD' | Phương thức thanh toán |
| PaymentStatus | VARCHAR(20) | NOT NULL, DEFAULT 'UNPAID' | Trạng thái thanh toán |
| Note | NVARCHAR(500) | NULL | Ghi chú đơn hàng |

**Ràng buộc bổ sung:**
- TotalAmount = Subtotal + ShippingFee.
- OrderStatus chỉ nhận: PENDING, CONFIRMED, PROCESSING, SHIPPING, COMPLETED, CANCELLED.
- PaymentMethod chỉ nhận: COD, BANK_TRANSFER, E_WALLET.
- PaymentStatus chỉ nhận: UNPAID, PAID, FAILED, REFUNDED.

### 4.7. Bảng OrderItems

**Mục đích:** Lưu các sản phẩm và số lượng thuộc từng đơn hàng.

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---|---|---|---|
| OrderItemId | BIGINT | PK, IDENTITY | Mã dòng chi tiết |
| OrderId | BIGINT | FK, NOT NULL | Đơn hàng tương ứng |
| VariantId | INT | FK, NOT NULL | Phiên bản sản phẩm |
| Quantity | INT | NOT NULL, CHECK > 0 | Số lượng mua |
| UnitPrice | DECIMAL(18,2) | NOT NULL, CHECK >= 0 | Đơn giá tại thời điểm mua |
| LineTotal | DECIMAL(18,2) | NOT NULL | Thành tiền dòng chi tiết |

**Ràng buộc bổ sung:** LineTotal = Quantity * UnitPrice.

### 4.8. Bảng Carts

**Mục đích:** Lưu giỏ hàng của người dùng.

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---|---|---|---|
| CartId | BIGINT | PK, IDENTITY | Mã giỏ hàng |
| UserId | INT | FK, NOT NULL, UNIQUE | Người sở hữu giỏ hàng |
| UpdatedAt | DATETIME2(0) | NOT NULL, DEFAULT SYSDATETIME() | Thời điểm cập nhật |

### 4.9. Bảng CartItems

**Mục đích:** Lưu các phiên bản sản phẩm được thêm vào giỏ hàng.

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---|---|---|---|
| CartItemId | BIGINT | PK, IDENTITY | Mã dòng giỏ hàng |
| CartId | BIGINT | FK, NOT NULL | Giỏ hàng tương ứng |
| VariantId | INT | FK, NOT NULL | Phiên bản sản phẩm |
| Quantity | INT | NOT NULL, CHECK > 0 | Số lượng trong giỏ |

**Ràng buộc bổ sung:** UNIQUE(CartId, VariantId), mỗi phiên bản chỉ xuất hiện một lần trong cùng giỏ hàng.

## 5. Quan hệ giữa các bảng

| Bảng cha | Bảng con | Quan hệ |
|---|---|---|
| Roles | Users | Một - nhiều |
| Users | Orders | Một - nhiều |
| Users | Carts | Một - không hoặc một |
| Categories | Products | Một - nhiều |
| Products | ProductVariants | Một - nhiều |
| Orders | OrderItems | Một - nhiều |
| ProductVariants | OrderItems | Một - nhiều |
| Carts | CartItems | Một - nhiều |
| ProductVariants | CartItems | Một - nhiều |

## 6. Quy tắc toàn vẹn dữ liệu

- Các khóa ngoại đảm bảo bản ghi liên quan phải tồn tại.
- Các khóa UNIQUE ngăn dữ liệu định danh bị trùng.
- Các CHECK constraint giới hạn giá trị hợp lệ của giá, số lượng và trạng thái.
- Các cột IDENTITY tự sinh mã định danh.
- Các DEFAULT constraint tự cung cấp giá trị mặc định cho những cột được quy định.

## 7. Lưu ý

Tài liệu này mô tả schema thiết kế của ClothingShopDB.
Trước khi nộp, cần đối chiếu lại với script SQL thực tế để bảo đảm
tên cột, kiểu dữ liệu, khóa và các ràng buộc khớp hoàn toàn.
