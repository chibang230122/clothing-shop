/*
 CLOTHING SHOP DATABASE - SQL Server
 Java Spring Boot + Spring Data JPA
 WARNING: This script drops and recreates ClothingShopDB. Use only for a new/test database.
*/
USE master;
GO
IF DB_ID(N'ClothingShopDB') IS NOT NULL
BEGIN
    ALTER DATABASE ClothingShopDB SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE ClothingShopDB;
END;
GO
CREATE DATABASE ClothingShopDB;
GO
USE ClothingShopDB;
GO

CREATE TABLE Roles (
                       RoleId INT IDENTITY(1,1) PRIMARY KEY,
                       RoleName NVARCHAR(30) NOT NULL UNIQUE
);
CREATE TABLE Users (
                       UserId INT IDENTITY(1,1) PRIMARY KEY,
                       RoleId INT NOT NULL,
                       FullName NVARCHAR(100) NOT NULL,
                       Email NVARCHAR(150) NOT NULL UNIQUE,
                       Phone VARCHAR(20) NULL,
                       GoogleSubject VARCHAR(255) NULL,
                       PasswordHash NVARCHAR(255) NOT NULL,
                       Address NVARCHAR(300) NULL,
                       IsActive BIT NOT NULL DEFAULT 1,
                       CreatedAt DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
                       CONSTRAINT FK_Users_Roles FOREIGN KEY (RoleId) REFERENCES Roles(RoleId)
);
CREATE UNIQUE INDEX UX_Users_GoogleSubject ON Users(GoogleSubject) WHERE GoogleSubject IS NOT NULL;
CREATE TABLE Categories (
                            CategoryId INT IDENTITY(1,1) PRIMARY KEY,
                            CategoryName NVARCHAR(100) NOT NULL UNIQUE,
                            Description NVARCHAR(500) NULL,
                            IsActive BIT NOT NULL DEFAULT 1
);
CREATE TABLE Products (
                          ProductId INT IDENTITY(1,1) PRIMARY KEY,
                          CategoryId INT NOT NULL,
                          ProductName NVARCHAR(200) NOT NULL,
                          Description NVARCHAR(MAX) NULL,
                          BasePrice DECIMAL(18,2) NOT NULL CHECK (BasePrice >= 0),
                          ImageUrl NVARCHAR(500) NULL,
                          IsActive BIT NOT NULL DEFAULT 1,
                          CreatedAt DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
                          CONSTRAINT FK_Products_Categories FOREIGN KEY (CategoryId) REFERENCES Categories(CategoryId)
);
CREATE TABLE ProductVariants (
                                 VariantId INT IDENTITY(1,1) PRIMARY KEY,
                                 ProductId INT NOT NULL,
                                 SKU VARCHAR(50) NOT NULL UNIQUE,
                                 Size NVARCHAR(10) NOT NULL,
                                 Color NVARCHAR(40) NOT NULL,
                                 StockQuantity INT NOT NULL DEFAULT 0 CHECK (StockQuantity >= 0),
                                 IsActive BIT NOT NULL DEFAULT 1,
                                 CONSTRAINT UQ_ProductVariants_Product_Size_Color UNIQUE (ProductId, Size, Color),
                                 CONSTRAINT FK_ProductVariants_Products FOREIGN KEY (ProductId) REFERENCES Products(ProductId)
);
CREATE TABLE Orders (
                        OrderId BIGINT IDENTITY(1,1) PRIMARY KEY,
                        UserId INT NOT NULL,
                        OrderCode VARCHAR(30) NOT NULL UNIQUE,
                        OrderDate DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
                        RecipientName NVARCHAR(100) NOT NULL,
                        RecipientPhone VARCHAR(20) NOT NULL,
                        ShippingAddress NVARCHAR(300) NOT NULL,
                        Subtotal DECIMAL(18,2) NOT NULL CHECK (Subtotal >= 0),
                        ShippingFee DECIMAL(18,2) NOT NULL DEFAULT 0 CHECK (ShippingFee >= 0),
                        TotalAmount DECIMAL(18,2) NOT NULL,
                        OrderStatus VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                            CHECK (OrderStatus IN ('PENDING','CONFIRMED','PROCESSING','SHIPPING','COMPLETED','CANCELLED')),
                        PaymentMethod VARCHAR(20) NOT NULL DEFAULT 'COD'
                            CHECK (PaymentMethod IN ('COD','BANK_TRANSFER','E_WALLET')),
                        PaymentStatus VARCHAR(20) NOT NULL DEFAULT 'UNPAID'
                            CHECK (PaymentStatus IN ('UNPAID','PAID','FAILED','REFUNDED')),
                        Note NVARCHAR(500) NULL,
                        CONSTRAINT CK_Orders_TotalAmount CHECK (TotalAmount = Subtotal + ShippingFee),
                        CONSTRAINT FK_Orders_Users FOREIGN KEY (UserId) REFERENCES Users(UserId)
);
CREATE TABLE OrderItems (
                            OrderItemId BIGINT IDENTITY(1,1) PRIMARY KEY,
                            OrderId BIGINT NOT NULL,
                            VariantId INT NOT NULL,
                            Quantity INT NOT NULL CHECK (Quantity > 0),
                            UnitPrice DECIMAL(18,2) NOT NULL CHECK (UnitPrice >= 0),
                            LineTotal DECIMAL(18,2) NOT NULL,
                            CONSTRAINT CK_OrderItems_LineTotal CHECK (LineTotal = Quantity * UnitPrice),
                            CONSTRAINT FK_OrderItems_Orders FOREIGN KEY (OrderId) REFERENCES Orders(OrderId),
                            CONSTRAINT FK_OrderItems_ProductVariants FOREIGN KEY (VariantId) REFERENCES ProductVariants(VariantId)
);
CREATE TABLE Carts (
                       CartId BIGINT IDENTITY(1,1) PRIMARY KEY,
                       UserId INT NOT NULL UNIQUE,
                       UpdatedAt DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
                       CONSTRAINT FK_Carts_Users FOREIGN KEY (UserId) REFERENCES Users(UserId)
);
CREATE TABLE CartItems (
                           CartItemId BIGINT IDENTITY(1,1) PRIMARY KEY,
                           CartId BIGINT NOT NULL,
                           VariantId INT NOT NULL,
                           Quantity INT NOT NULL CHECK (Quantity > 0),
                           CONSTRAINT UQ_CartItems_Cart_Variant UNIQUE (CartId, VariantId),
                           CONSTRAINT FK_CartItems_Carts FOREIGN KEY (CartId) REFERENCES Carts(CartId),
                           CONSTRAINT FK_CartItems_ProductVariants FOREIGN KEY (VariantId) REFERENCES ProductVariants(VariantId)
);
CREATE TABLE ImportReceipts (
                                ImportReceiptId INT IDENTITY(1,1) PRIMARY KEY,
                                ImportDate DATETIME2(0) NOT NULL
        DEFAULT SYSDATETIME(),
                                TotalAmount DECIMAL(18,2) NOT NULL
                                    DEFAULT 0 CHECK (TotalAmount >= 0),
                                Note NVARCHAR(500) NULL,
                                CreatedBy INT NOT NULL,

                                CONSTRAINT FK_ImportReceipts_Users
                                    FOREIGN KEY (CreatedBy)
                                        REFERENCES Users(UserId)
);

CREATE TABLE ImportReceiptDetails (
                                      ImportDetailId INT IDENTITY(1,1) PRIMARY KEY,
                                      ImportReceiptId INT NOT NULL,
                                      VariantId INT NOT NULL,
                                      Quantity INT NOT NULL CHECK (Quantity > 0),
                                      ImportPrice DECIMAL(18,2) NOT NULL
                                          CHECK (ImportPrice >= 0),
                                      Subtotal DECIMAL(18,2) NOT NULL
                                          CHECK (Subtotal >= 0),

                                      CONSTRAINT CK_ImportReceiptDetails_Subtotal
                                          CHECK (Subtotal = Quantity * ImportPrice),

                                      CONSTRAINT FK_ImportReceiptDetails_Receipts
                                          FOREIGN KEY (ImportReceiptId)
                                              REFERENCES ImportReceipts(ImportReceiptId),

                                      CONSTRAINT FK_ImportReceiptDetails_Variants
                                          FOREIGN KEY (VariantId)
                                              REFERENCES ProductVariants(VariantId)
);
GO


CREATE INDEX IX_Users_RoleId ON Users(RoleId);
CREATE INDEX IX_Products_CategoryId_IsActive ON Products(CategoryId, IsActive);
CREATE INDEX IX_Products_ProductName ON Products(ProductName);
CREATE INDEX IX_Products_BasePrice ON Products(BasePrice);
CREATE INDEX IX_ProductVariants_ProductId ON ProductVariants(ProductId);
CREATE INDEX IX_ProductVariants_StockQuantity ON ProductVariants(StockQuantity);
CREATE INDEX IX_Orders_UserId_OrderDate ON Orders(UserId, OrderDate DESC);
CREATE INDEX IX_Orders_OrderStatus_OrderDate ON Orders(OrderStatus, OrderDate DESC);
CREATE INDEX IX_OrderItems_OrderId ON OrderItems(OrderId);
CREATE INDEX IX_OrderItems_VariantId ON OrderItems(VariantId);
CREATE INDEX IX_CartItems_VariantId ON CartItems(VariantId);
CREATE INDEX IX_ImportReceipts_ImportDate ON ImportReceipts(ImportDate);
CREATE INDEX IX_ImportReceiptDetails_ReceiptId ON ImportReceiptDetails(ImportReceiptId);
CREATE INDEX IX_ImportReceiptDetails_VariantId ON ImportReceiptDetails(VariantId);
GO

INSERT INTO Roles (RoleName) VALUES (N'ADMIN'),(N'STAFF'),(N'CUSTOMER');

-- Demo-only placeholder hashes. Replace with BCrypt hashes generated by your Java application.
INSERT INTO Users (RoleId,FullName,Email,Phone,PasswordHash,Address,IsActive) VALUES
                                                                                  (1,N'Quản trị viên','admin@clothingshop.local','0900000001',N'REPLACE_WITH_BCRYPT_HASH',N'TP. Hồ Chí Minh',1),
                                                                                  (2,N'Nhân viên 01','staff01@clothingshop.local','0900000002',N'REPLACE_WITH_BCRYPT_HASH',N'TP. Hồ Chí Minh',1),
                                                                                  (3,N'Nguyễn An','customer01@clothingshop.local','0900000003',N'REPLACE_WITH_BCRYPT_HASH',N'Trà Vinh',1),
                                                                                  (3,N'Trần Bình','customer02@clothingshop.local','0900000004',N'REPLACE_WITH_BCRYPT_HASH',N'Cần Thơ',1),
                                                                                  (3,N'Lê Chi','customer03@clothingshop.local','0900000005',N'REPLACE_WITH_BCRYPT_HASH',N'Vĩnh Long',1),
                                                                                  (3,N'Phạm Dũng','customer04@clothingshop.local','0900000006',N'REPLACE_WITH_BCRYPT_HASH',N'Tiền Giang',1),
                                                                                  (3,N'Hoàng Em','customer05@clothingshop.local','0900000007',N'REPLACE_WITH_BCRYPT_HASH',N'Bến Tre',1),
                                                                                  (3,N'Võ Giang','customer06@clothingshop.local','0900000008',N'REPLACE_WITH_BCRYPT_HASH',N'Sóc Trăng',1),
                                                                                  (3,N'Đặng Hạnh','customer07@clothingshop.local','0900000009',N'REPLACE_WITH_BCRYPT_HASH',N'Long An',1),
                                                                                  (3,N'Bùi Khang','customer08@clothingshop.local','0900000010',N'REPLACE_WITH_BCRYPT_HASH',N'Đồng Tháp',1),
                                                                                  (3,N'Đỗ Lan','customer09@clothingshop.local','0900000011',N'REPLACE_WITH_BCRYPT_HASH',N'An Giang',1),
                                                                                  (3,N'Ngô Minh','customer10@clothingshop.local','0900000012',N'REPLACE_WITH_BCRYPT_HASH',N'Kiên Giang',1),
                                                                                  (3,N'Dương Nam','customer11@clothingshop.local','0900000013',N'REPLACE_WITH_BCRYPT_HASH',N'Cà Mau',1),
                                                                                  (3,N'Vũ Oanh','customer12@clothingshop.local','0900000014',N'REPLACE_WITH_BCRYPT_HASH',N'Bạc Liêu',1),
                                                                                  (3,N'Phan Phúc','customer13@clothingshop.local','0900000015',N'REPLACE_WITH_BCRYPT_HASH',N'Đồng Nai',1),
                                                                                  (3,N'Đinh Quỳnh','customer14@clothingshop.local','0900000016',N'REPLACE_WITH_BCRYPT_HASH',N'Bình Dương',1),
                                                                                  (3,N'Vương Sơn','customer15@clothingshop.local','0900000017',N'REPLACE_WITH_BCRYPT_HASH',N'Bình Phước',1),
                                                                                  (3,N'Lý Thảo','customer16@clothingshop.local','0900000018',N'REPLACE_WITH_BCRYPT_HASH',N'Lâm Đồng',1),
                                                                                  (3,N'Mai Uyên','customer17@clothingshop.local','0900000019',N'REPLACE_WITH_BCRYPT_HASH',N'Đắk Lắk',1),
                                                                                  (3,N'Tạ Việt','customer18@clothingshop.local','0900000020',N'REPLACE_WITH_BCRYPT_HASH',N'Gia Lai',1),
                                                                                  (3,N'Chu Yến','customer19@clothingshop.local','0900000021',N'REPLACE_WITH_BCRYPT_HASH',N'Kon Tum',1),
                                                                                  (3,N'Đào Khôi','customer20@clothingshop.local','0900000022',N'REPLACE_WITH_BCRYPT_HASH',N'TP. Hồ Chí Minh',1);

INSERT INTO Categories (CategoryName,Description) VALUES
                                                      (N'Áo thun',N'Áo thun nam nữ'),
                                                      (N'Áo sơ mi',N'Áo sơ mi công sở và thường ngày'),
                                                      (N'Quần jean',N'Quần jean nhiều kiểu dáng'),
                                                      (N'Áo khoác',N'Áo khoác ngoài'),
                                                      (N'Quần kaki',N'Quần kaki đi học và đi làm'),
                                                      (N'Đầm',N'Đầm thời trang nữ'),
                                                      (N'Áo hoodie',N'Áo hoodie và áo nỉ'),
                                                      (N'Phụ kiện',N'Phụ kiện thời trang');

INSERT INTO Products (CategoryId,ProductName,Description,BasePrice,ImageUrl) VALUES
                                                                                 (1,N'Áo thun basic trắng',N'Áo thun cotton basic',159000,N'/images/ao-thun-basic-trang.jpg'),
                                                                                 (1,N'Áo thun basic đen',N'Áo thun cotton màu đen',159000,N'/images/ao-thun-basic-den.jpg'),
                                                                                 (1,N'Áo thun oversize xanh',N'Áo thun oversize',199000,N'/images/ao-thun-oversize-xanh.jpg'),
                                                                                 (2,N'Áo sơ mi Oxford',N'Áo sơ mi tay dài',329000,N'/images/so-mi-oxford.jpg'),
                                                                                 (2,N'Áo sơ mi linen',N'Áo sơ mi linen thoáng mát',359000,N'/images/so-mi-linen.jpg'),
                                                                                 (2,N'Áo sơ mi caro',N'Áo sơ mi họa tiết caro',299000,N'/images/so-mi-caro.jpg'),
                                                                                 (3,N'Quần jean slim fit',N'Quần jean dáng slim fit',429000,N'/images/jean-slim.jpg'),
                                                                                 (3,N'Quần jean straight',N'Quần jean dáng đứng',449000,N'/images/jean-straight.jpg'),
                                                                                 (3,N'Quần jean wide leg',N'Quần jean ống rộng',469000,N'/images/jean-wide.jpg'),
                                                                                 (4,N'Áo khoác denim',N'Áo khoác denim',599000,N'/images/khoac-denim.jpg'),
                                                                                 (4,N'Áo khoác gió',N'Áo khoác gió nhẹ',399000,N'/images/khoac-gio.jpg'),
                                                                                 (4,N'Áo khoác bomber',N'Áo khoác bomber',649000,N'/images/khoac-bomber.jpg'),
                                                                                 (5,N'Quần kaki slim',N'Quần kaki dáng slim',359000,N'/images/kaki-slim.jpg'),
                                                                                 (5,N'Quần kaki straight',N'Quần kaki dáng đứng',379000,N'/images/kaki-straight.jpg'),
                                                                                 (5,N'Quần short kaki',N'Quần short kaki',259000,N'/images/short-kaki.jpg'),
                                                                                 (6,N'Đầm suông tối giản',N'Đầm suông',429000,N'/images/dam-suong.jpg'),
                                                                                 (6,N'Đầm hoa mùa hè',N'Đầm hoa',459000,N'/images/dam-hoa.jpg'),
                                                                                 (6,N'Đầm dự tiệc',N'Đầm dự tiệc',699000,N'/images/dam-du-tiec.jpg'),
                                                                                 (7,N'Hoodie basic',N'Áo hoodie basic',399000,N'/images/hoodie-basic.jpg'),
                                                                                 (7,N'Hoodie zip',N'Áo hoodie khóa kéo',449000,N'/images/hoodie-zip.jpg'),
                                                                                 (7,N'Áo nỉ cổ tròn',N'Áo nỉ cổ tròn',329000,N'/images/ao-ni.jpg'),
                                                                                 (8,N'Nón lưỡi trai',N'Nón lưỡi trai basic',129000,N'/images/non-luoi-trai.jpg'),
                                                                                 (8,N'Thắt lưng da tổng hợp',N'Thắt lưng thời trang',189000,N'/images/that-lung.jpg'),
                                                                                 (8,N'Túi tote canvas',N'Túi tote canvas',149000,N'/images/tui-tote.jpg');

-- Six variants per product: S/M/L x Black/White = 144 rows.
INSERT INTO ProductVariants (ProductId,SKU,Size,Color,StockQuantity,IsActive)
SELECT p.ProductId,
       CONCAT('SKU-',RIGHT(CONCAT('000',p.ProductId),3),'-',s.SizeCode,'-',c.ColorCode),
       s.SizeName,c.ColorName,10 + ((p.ProductId+s.SizeOrder+c.ColorOrder)%25),1
FROM Products p
    CROSS JOIN (VALUES (N'S','S',1),(N'M','M',2),(N'L','L',3)) s(SizeName,SizeCode,SizeOrder)
    CROSS JOIN (VALUES (N'Đen','BLK',1),(N'Trắng','WHT',2)) c(ColorName,ColorCode,ColorOrder);

INSERT INTO Carts (UserId) SELECT UserId FROM Users;

INSERT INTO CartItems (CartId,VariantId,Quantity)
SELECT c.CartId,v.VariantId,1+(u.UserId%3)
FROM Users u
         JOIN Carts c ON c.UserId=u.UserId
         JOIN ProductVariants v ON v.ProductId=u.UserId AND v.Size=N'L' AND v.Color=N'Trắng'
WHERE u.UserId BETWEEN 3 AND 22;

-- 20 sample orders; one order line per order so totals match exactly.
INSERT INTO Orders
(UserId,OrderCode,OrderDate,RecipientName,RecipientPhone,ShippingAddress,
 Subtotal,ShippingFee,TotalAmount,OrderStatus,PaymentMethod,PaymentStatus,Note)
SELECT u.UserId,
       CONCAT('ORD-',RIGHT(CONCAT('0000',u.UserId-2),4)),
       DATEADD(DAY,-(u.UserId-2),SYSDATETIME()),
       u.FullName,u.Phone,COALESCE(u.Address,N'Chưa cập nhật'),
       p.BasePrice * CASE WHEN u.UserId%2=0 THEN 2 ELSE 1 END,
       30000,
       p.BasePrice * CASE WHEN u.UserId%2=0 THEN 2 ELSE 1 END + 30000,
       CASE WHEN u.UserId<=12 THEN 'COMPLETED' ELSE 'PENDING' END,
       CASE WHEN u.UserId%2=0 THEN 'BANK_TRANSFER' ELSE 'COD' END,
       CASE WHEN u.UserId<=12 THEN 'PAID' ELSE 'UNPAID' END,
       N'Dữ liệu mẫu kiểm thử'
FROM Users u
         JOIN Products p ON p.ProductId=u.UserId-2
WHERE u.UserId BETWEEN 3 AND 22;

INSERT INTO OrderItems (OrderId,VariantId,Quantity,UnitPrice,LineTotal)
SELECT o.OrderId,v.VariantId,
       CASE WHEN o.UserId%2=0 THEN 2 ELSE 1 END,
       p.BasePrice,
       p.BasePrice * CASE WHEN o.UserId%2=0 THEN 2 ELSE 1 END
FROM Orders o
         JOIN Products p ON p.ProductId=o.UserId-2
         JOIN ProductVariants v ON v.ProductId=p.ProductId AND v.Size=N'M' AND v.Color=N'Đen';
GO

/* =========================================================
   CUSTOM QUERIES AND VALIDATION
   ========================================================= */
DECLARE @Keyword NVARCHAR(100)=N'áo';
DECLARE @MinPrice DECIMAL(18,2)=100000;
DECLARE @MaxPrice DECIMAL(18,2)=500000;

SELECT p.ProductId,p.ProductName,c.CategoryName,p.BasePrice
FROM Products p
         JOIN Categories c ON c.CategoryId=p.CategoryId
WHERE p.IsActive=1 AND c.IsActive=1
  AND p.BasePrice BETWEEN @MinPrice AND @MaxPrice
  AND (p.ProductName LIKE N'%'+@Keyword+N'%' OR c.CategoryName LIKE N'%'+@Keyword+N'%')
ORDER BY p.ProductName;

SELECT CAST(OrderDate AS DATE) AS OrderDay,
       COUNT(*) AS TotalOrders,
       SUM(TotalAmount) AS Revenue
FROM Orders
WHERE OrderStatus='COMPLETED' AND PaymentStatus='PAID'
GROUP BY CAST(OrderDate AS DATE)
ORDER BY OrderDay DESC;

SELECT 'Roles' AS TableName,COUNT(*) AS TotalRows FROM Roles
UNION ALL SELECT 'Users',COUNT(*) FROM Users
UNION ALL SELECT 'Categories',COUNT(*) FROM Categories
UNION ALL SELECT 'Products',COUNT(*) FROM Products
UNION ALL SELECT 'ProductVariants',COUNT(*) FROM ProductVariants
UNION ALL SELECT 'Orders',COUNT(*) FROM Orders
UNION ALL SELECT 'OrderItems',COUNT(*) FROM OrderItems
UNION ALL SELECT 'Carts',COUNT(*) FROM Carts
UNION ALL SELECT 'CartItems',COUNT(*) FROM CartItems
UNION ALL SELECT 'ImportReceipts', COUNT(*) FROM ImportReceipts
UNION ALL SELECT 'ImportReceiptDetails', COUNT(*) FROM ImportReceiptDetails
ORDER BY TableName;
GO