# ĐỒ ÁN CÔNG NGHỆ JAVA --- WEBSITE QUẢN LÝ SHOP BÁN QUẦN ÁO

> Tài liệu kế hoạch triển khai cho nhóm 5 thành viên. Bám theo rubric đồ
> án: Spring Boot, Thymeleaf, Bootstrap, SQL Server, Spring Data JPA, đa
> luồng, lập trình mạng, báo cáo và Git.

## 1. Mục tiêu và phạm vi

Xây dựng website quản lý shop bán quần áo, hỗ trợ khách hàng xem sản
phẩm và đặt hàng; nhân viên xử lý đơn hàng; quản lý quản trị sản phẩm,
kho, nhân viên và báo cáo. Ứng dụng phải chạy được khi demo, kết nối SQL
Server qua Spring Data JPA, có một nghiệp vụ đa luồng được kiểm thử và
có chương trình client riêng giao tiếp với server qua Socket.

### Công nghệ đề xuất

-   Java: chọn một phiên bản JDK tương thích với phiên bản Spring Boot
    nhóm dùng; ghi rõ trong README.
-   Spring Boot: Spring Web, Thymeleaf, Spring Data JPA, Validation,
    Spring Security.
-   Giao diện: Thymeleaf + Bootstrap 5 + CSS/JavaScript.
-   Database: Microsoft SQL Server.
-   Mạng: Java `ServerSocket`/`Socket`, giao thức JSON hoặc văn bản có
    cấu trúc.
-   Đa luồng: `ExecutorService` và cơ chế đồng bộ thích hợp.
-   Git: GitHub hoặc GitLab; mỗi thành viên commit phần mình.
-   Kiểm thử: JUnit, Spring Boot Test và kịch bản kiểm thử Socket.

## 2. Rubric của đề và những thứ cần có trong code

  -------------------------------------------------------------------------------------------------
  Mã               Tiêu chí                                          Điểm tối đa Bằng chứng cần
                                                                                 trình bày
  ---------------- --------------------------------------- --------------------- ------------------
  1.1              Mô tả bài toán, nghiệp vụ, đối tượng,                     0,4 Báo cáo chương 1
                   phạm vi                                                       

  1.2              Tối thiểu 10 chức năng, chia theo vai                     0,4 Bảng chức năng/use
                   trò; yêu cầu phi chức năng                                    case

  1.3              ERD, bảng, khóa chính/ngoại, ràng buộc                    0,4 ERD và data
                                                                                 dictionary

  1.4              Kiến trúc                                                 0,4 Sơ đồ kiến trúc,
                   Controller--Service--Repository--View                         mã nguồn
                   và luồng request                                              

  1.5              Thiết kế đa luồng, tài nguyên chung,                      0,4 Mục thiết kế riêng
                   giao thức mạng                                                và demo

  2.1              Package rõ ràng, cấu hình ứng dụng                        0,3 Mã nguồn

  2.2              Mapping Controller, truyền dữ liệu,                       0,4 Mã nguồn và demo
                   validate                                                      

  2.3              Nghiệp vụ trong Service,                                  0,4 Mã nguồn
                   `@Transactional` phù hợp                                      

  2.4              Chạy được chức năng đã đặc tả                             0,3 Demo theo bảng
                                                                                 chức năng

  2.5              Trang lỗi và xử lý ngoại lệ                               0,2 Demo tình huống
                                                                                 lỗi

  3.1              Thymeleaf fragments: header, menu,                        0,3 Template dùng
                   footer                                                        chung

  3.2              Bootstrap, responsive                                     0,3 Demo
                                                                                 desktop/mobile

  3.3              Form có validate, báo thành công, xác                     0,4 Demo
                   nhận xóa                                                      

  3.4              Tìm kiếm, phân trang, sắp xếp                             0,4 Demo danh sách

  3.5              UI nhất quán, tiếng Việt đúng, menu                       0,2 Demo theo tài
                   theo vai trò                                                  khoản

  4.1              SQL Server, script schema và dữ liệu                      0,2 File SQL và cấu
                   mẫu                                                           hình

  4.2              Entity ánh xạ đúng, quan hệ ít nhất 1-n                   0,4 Entity và ERD

  4.3              Repository có ít nhất 2 truy vấn tùy                      0,3 `@Query`/derived
                   chỉnh                                                         query

  4.4              CRUD tối thiểu 3 bảng chính                               0,4 Demo và kiểm tra
                                                                                 SQL Server

  4.5              Transaction, ràng buộc, không xóa bản                     0,3 Demo lỗi và xử lý
                   ghi đang được tham chiếu                                      

  5.1              Tác vụ đa luồng có ý nghĩa nghiệp vụ                      0,4 Log/tên luồng và
                                                                                 demo

  5.2              Đồng bộ tài nguyên dùng chung, test                       0,4 Test và kết quả
                   nhiều luồng                                                   

  5.3              Client riêng giao tiếp với server qua                     0,5 Demo client-server
                   URL/Socket/RMI                                                

  5.4              Server phục vụ tối thiểu 3 client cùng                    0,3 Demo 3 client
                   lúc                                                           

  6.1              Báo cáo đúng cấu trúc                                     0,4 Báo cáo 25--40
                                                                                 trang

  6.2              Thuyết trình 15 phút, demo chức năng                      0,4 Slide tối đa 20
                   chính                                                         trang

  6.3              Mỗi người giải thích được code mình phụ                   0,4 Vấn đáp
                   trách                                                         

  6.4              Phân công, commit Git thường xuyên                        0,4 Bảng phân công,
                                                                                 lịch sử Git
  -------------------------------------------------------------------------------------------------

**Điều kiện giới hạn điểm trong đề:** ứng dụng không chạy khi demo thì
tiêu chí 2--5 tối đa mức D; không có đa luồng thì 5.1 và 5.2 bằng 0;
không có lập trình mạng thì 5.3 và 5.4 bằng 0; chỉ dùng JDBC thuần thay
Spring Data JPA thì tiêu chí 4 tối đa mức C; nộp trễ bị trừ điểm theo
quy định trong đề.

## 3. Chức năng website theo vai trò

### 3.1 Khách vãng lai

1.  Xem trang chủ, sản phẩm nổi bật và sản phẩm mới.
2.  Xem danh sách sản phẩm.
3.  Tìm kiếm theo tên/mã sản phẩm.
4.  Lọc theo danh mục, giới tính/phong cách nếu nhóm có dùng, kích cỡ,
    màu, khoảng giá.
5.  Sắp xếp theo giá, tên hoặc mới nhất.
6.  Xem chi tiết sản phẩm, ảnh, mô tả, giá, kích cỡ/màu và tồn kho khả
    dụng.
7.  Đăng ký tài khoản.
8.  Đăng nhập.

### 3.2 Khách hàng đã đăng nhập

1.  Cập nhật thông tin cá nhân.
2.  Thêm/xóa/cập nhật số lượng trong giỏ hàng.
3.  Đặt hàng và nhập địa chỉ nhận hàng.
4.  Chọn phương thức thanh toán được hỗ trợ trong phạm vi đồ án (nên bắt
    đầu bằng COD).
5.  Xem lịch sử đơn hàng.
6.  Xem chi tiết đơn hàng và trạng thái.
7.  Hủy đơn khi đơn còn ở trạng thái cho phép.
8.  Đổi mật khẩu/đăng xuất.

### 3.3 Nhân viên bán hàng

1.  Xem danh sách đơn hàng được xử lý.
2.  Xác nhận đơn, chuyển trạng thái theo quy trình.
3.  Cập nhật trạng thái giao hàng.
4.  Tra cứu sản phẩm và tồn kho.
5.  Tạo đơn tại quầy nếu nhóm quyết định có chức năng bán trực tiếp.
6.  Không được tự tạo tài khoản Admin hoặc tự nâng quyền.

### 3.4 Quản lý/Admin

1.  Dashboard: tổng đơn, doanh thu theo khoảng thời gian, sản phẩm sắp
    hết hàng.
2.  CRUD danh mục.
3.  CRUD sản phẩm và biến thể (size/màu).
4.  Quản lý ảnh sản phẩm.
5.  Quản lý tồn kho và lịch sử nhập/xuất.
6.  Quản lý đơn hàng và chi tiết đơn.
7.  Quản lý khách hàng/tài khoản, khóa hoặc mở tài khoản theo quy định.
8.  Quản lý nhân viên và vai trò (chỉ Admin).
9.  Xem báo cáo doanh thu/sản phẩm bán chạy.
10. Tìm kiếm, lọc, phân trang và sắp xếp danh sách quản trị.
11. Xem nhật ký thao tác quan trọng nếu kịp triển khai.

### 3.5 Chức năng tối thiểu nên chốt để không bị quá tải

Ưu tiên hoàn thành: đăng nhập/phân quyền; danh mục; sản phẩm và biến
thể; tìm kiếm/lọc; giỏ hàng; đặt hàng; quản lý đơn; tồn kho; báo cáo cơ
bản; đa luồng; Socket server/client. Đánh giá, mã giảm giá, email, thanh
toán trực tuyến, vận chuyển thật và chat là phần mở rộng, chỉ làm khi
các yêu cầu rubric đã chạy ổn định.

## 4. Quy tắc nghiệp vụ cần thống nhất

1.  Mã sản phẩm/sku là duy nhất.
2.  Giá và số lượng không âm; số lượng đặt phải lớn hơn 0.
3.  Một sản phẩm có thể có nhiều biến thể size/màu; tồn kho quản lý theo
    biến thể, không chỉ theo tên sản phẩm.
4.  Đơn hàng lưu giá tại thời điểm đặt hàng trong
    `OrderItems.UnitPrice`, không phụ thuộc giá sản phẩm thay đổi sau
    đó.
5.  Tổng tiền được tính ở phía server từ dữ liệu database, không tin
    tổng tiền gửi từ trình duyệt.
6.  Khi đặt hàng thành công, kiểm tra tồn kho và giảm tồn kho trong một
    transaction; không cho số lượng tồn âm.
7.  Nếu tạo đơn thất bại thì không được để tồn kho bị trừ dở dang.
8.  Không xóa cứng danh mục/sản phẩm đã được đơn hàng tham chiếu; dùng
    `IsActive` hoặc từ chối xóa.
9.  Chỉ chủ đơn hàng hoặc nhân viên/Admin đúng quyền mới xem được thông
    tin đơn.
10. Khách không tự đổi trạng thái đơn thành "Đã giao" hay "Đã thanh
    toán".
11. Quy định chuyển trạng thái đơn rõ ràng, ví dụ:
    `Pending → Confirmed → Shipping → Completed`; có thể chuyển sang
    `Cancelled` theo điều kiện cho phép.
12. Mọi kiểm tra quyền phải thực hiện ở backend, không chỉ ẩn nút trên
    giao diện.

## 5. Thiết kế database đề xuất

Thiết kế dưới đây gồm 9 bảng chính. Có thể thêm bảng khi cần, nhưng nên
thống nhất schema trước khi mỗi người code. Tên bảng dùng tiếng Anh nhất
quán để thuận lợi khi viết Entity.

### 5.1 Sơ đồ quan hệ dạng chữ

``` text
Roles 1 ─── n Users
Users 1 ─── n Orders
Categories 1 ─── n Products
Products 1 ─── n ProductVariants
Orders 1 ─── n OrderItems
ProductVariants 1 ─── n OrderItems
Users 1 ─── 0..1 Carts
Carts 1 ─── n CartItems
ProductVariants 1 ─── n CartItems
```

Các quan hệ `Carts` có thể được thiết kế theo cách khác nếu nhóm muốn
giỏ hàng tồn tại trong session; phương án database giúp lưu giỏ hàng qua
lần đăng nhập. Với thiết kế này, mỗi user chỉ nên có một giỏ hàng đang
hoạt động.

### 5.2 Bảng `Roles` --- Vai trò

  Cột        Kiểu SQL Server   Quy tắc
  ---------- ----------------- ------------------
  RoleId     INT IDENTITY      PK
  RoleName   NVARCHAR(30)      NOT NULL, UNIQUE

Dữ liệu gợi ý: `ADMIN`, `STAFF`, `CUSTOMER`.

### 5.3 Bảng `Users` --- Tài khoản

  Cột            Kiểu SQL Server   Quy tắc
  -------------- ----------------- --------------------------------------
  UserId         INT IDENTITY      PK
  RoleId         INT               FK → Roles
  FullName       NVARCHAR(100)     NOT NULL
  Email          NVARCHAR(150)     NOT NULL, UNIQUE
  Phone          VARCHAR(20)       Có thể UNIQUE nếu chính sách yêu cầu
  PasswordHash   NVARCHAR(255)     NOT NULL
  Address        NVARCHAR(300)     Có thể NULL
  IsActive       BIT               NOT NULL, mặc định 1
  CreatedAt      DATETIME2         NOT NULL

Không lưu mật khẩu dạng chữ thường. Dùng Spring Security PasswordEncoder
để lưu hash mật khẩu.

### 5.4 Bảng `Categories` --- Danh mục

  Cột            Kiểu SQL Server   Quy tắc
  -------------- ----------------- ----------------------
  CategoryId     INT IDENTITY      PK
  CategoryName   NVARCHAR(100)     NOT NULL
  Description    NVARCHAR(500)     NULL
  IsActive       BIT               NOT NULL, mặc định 1

Ví dụ dữ liệu: Áo thun, Áo sơ mi, Quần jean, Quần kaki, Áo khoác.

### 5.5 Bảng `Products` --- Sản phẩm

  Cột           Kiểu SQL Server   Quy tắc
  ------------- ----------------- ----------------------
  ProductId     INT IDENTITY      PK
  CategoryId    INT               FK → Categories
  ProductName   NVARCHAR(200)     NOT NULL
  Description   NVARCHAR(MAX)     NULL
  BasePrice     DECIMAL(18,2)     NOT NULL, \>= 0
  ImageUrl      NVARCHAR(500)     NULL
  IsActive      BIT               NOT NULL, mặc định 1
  CreatedAt     DATETIME2         NOT NULL

`BasePrice` có thể là giá niêm yết; giá bán thực tế của từng biến
thể/đơn hàng cần được quy định rõ. Để đơn giản, dùng `BasePrice` làm giá
mặc định cho các biến thể; `OrderItems.UnitPrice` lưu giá chốt lúc mua.

### 5.6 Bảng `ProductVariants` --- Biến thể size/màu và tồn kho

  Cột             Kiểu SQL Server   Quy tắc
  --------------- ----------------- ----------------------
  VariantId       INT IDENTITY      PK
  ProductId       INT               FK → Products
  SKU             VARCHAR(50)       NOT NULL, UNIQUE
  Size            NVARCHAR(10)      NOT NULL
  Color           NVARCHAR(40)      NOT NULL
  StockQuantity   INT               NOT NULL, \>= 0
  IsActive        BIT               NOT NULL, mặc định 1

Nên tạo ràng buộc UNIQUE cho tổ hợp `(ProductId, Size, Color)` để tránh
nhập trùng biến thể. Tồn kho được theo dõi theo biến thể, ví dụ áo thun
đen size M có tồn kho khác áo thun đen size L.

### 5.7 Bảng `Orders` --- Đơn hàng

  Cột               Kiểu SQL Server   Quy tắc
  ----------------- ----------------- ----------------------
  OrderId           BIGINT IDENTITY   PK
  UserId            INT               FK → Users
  OrderCode         VARCHAR(30)       NOT NULL, UNIQUE
  OrderDate         DATETIME2         NOT NULL
  RecipientName     NVARCHAR(100)     NOT NULL
  RecipientPhone    VARCHAR(20)       NOT NULL
  ShippingAddress   NVARCHAR(300)     NOT NULL
  Subtotal          DECIMAL(18,2)     NOT NULL
  ShippingFee       DECIMAL(18,2)     NOT NULL, mặc định 0
  TotalAmount       DECIMAL(18,2)     NOT NULL
  OrderStatus       VARCHAR(20)       NOT NULL
  PaymentMethod     VARCHAR(20)       NOT NULL
  PaymentStatus     VARCHAR(20)       NOT NULL
  Note              NVARCHAR(500)     NULL

Trạng thái mẫu: `PENDING`, `CONFIRMED`, `SHIPPING`, `COMPLETED`,
`CANCELLED`. Phương thức thanh toán có thể là `COD`; trạng thái thanh
toán như `UNPAID`, `PAID`. Cần validate trạng thái ở backend.

### 5.8 Bảng `OrderItems` --- Chi tiết đơn hàng

  Cột           Kiểu SQL Server   Quy tắc
  ------------- ----------------- ----------------------
  OrderItemId   BIGINT IDENTITY   PK
  OrderId       BIGINT            FK → Orders
  VariantId     INT               FK → ProductVariants
  Quantity      INT               NOT NULL, \> 0
  UnitPrice     DECIMAL(18,2)     NOT NULL, \>= 0
  LineTotal     DECIMAL(18,2)     NOT NULL

`LineTotal = Quantity × UnitPrice`. Nên tính lại phía server. Khi biến
thể đã được tham chiếu trong đơn hàng, không xóa cứng biến thể đó.

### 5.9 Bảng `Carts` --- Giỏ hàng

  Cột         Kiểu SQL Server   Quy tắc
  ----------- ----------------- --------------------
  CartId      BIGINT IDENTITY   PK
  UserId      INT               FK → Users, UNIQUE
  UpdatedAt   DATETIME2         NOT NULL

### 5.10 Bảng `CartItems` --- Chi tiết giỏ hàng

  Cột          Kiểu SQL Server   Quy tắc
  ------------ ----------------- ----------------------
  CartItemId   BIGINT IDENTITY   PK
  CartId       BIGINT            FK → Carts
  VariantId    INT               FK → ProductVariants
  Quantity     INT               NOT NULL, \> 0

Tạo UNIQUE cho `(CartId, VariantId)` để mỗi biến thể chỉ có một dòng
trong giỏ. Khi thêm cùng biến thể, cập nhật số lượng thay vì tạo dòng
trùng.

### 5.11 Bảng mở rộng nên cân nhắc: `StockTransactions`

Nếu muốn chứng minh lịch sử nhập/xuất kho, thêm bảng:

  -----------------------------------------------------------------------
  Cột                     Kiểu SQL Server         Quy tắc
  ----------------------- ----------------------- -----------------------
  StockTransactionId      BIGINT IDENTITY         PK

  VariantId               INT                     FK → ProductVariants

  UserId                  INT                     FK → Users

  TransactionType         VARCHAR(20)             `IMPORT`, `SALE`,
                                                  `CANCEL_RETURN`,
                                                  `ADJUST`

  QuantityChange          INT                     Số lượng thay đổi có
                                                  dấu

  ReferenceCode           VARCHAR(50)             Mã đơn/phiếu liên quan

  Note                    NVARCHAR(300)           NULL

  CreatedAt               DATETIME2               NOT NULL
  -----------------------------------------------------------------------

Bảng này hữu ích cho chức năng quản lý kho nhưng có thể để giai đoạn mở
rộng nếu thời gian hạn chế.

## 6. Script SQL khởi tạo database

Đây là script khởi tạo schema mẫu cho 9 bảng ở trên. Chạy trong SQL
Server Management Studio (SSMS). Hãy dùng database riêng cho đồ án;
không chạy trên database có dữ liệu cần giữ.

``` sql
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
    PasswordHash NVARCHAR(255) NOT NULL,
    Address NVARCHAR(300) NULL,
    IsActive BIT NOT NULL DEFAULT 1,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Users_Roles FOREIGN KEY (RoleId) REFERENCES Roles(RoleId)
);

CREATE TABLE Categories (
    CategoryId INT IDENTITY(1,1) PRIMARY KEY,
    CategoryName NVARCHAR(100) NOT NULL,
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
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
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
    CONSTRAINT FK_ProductVariants_Products FOREIGN KEY (ProductId) REFERENCES Products(ProductId),
    CONSTRAINT UQ_ProductVariants_Product_Size_Color UNIQUE (ProductId, Size, Color)
);

CREATE TABLE Orders (
    OrderId BIGINT IDENTITY(1,1) PRIMARY KEY,
    UserId INT NOT NULL,
    OrderCode VARCHAR(30) NOT NULL UNIQUE,
    OrderDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    RecipientName NVARCHAR(100) NOT NULL,
    RecipientPhone VARCHAR(20) NOT NULL,
    ShippingAddress NVARCHAR(300) NOT NULL,
    Subtotal DECIMAL(18,2) NOT NULL CHECK (Subtotal >= 0),
    ShippingFee DECIMAL(18,2) NOT NULL DEFAULT 0 CHECK (ShippingFee >= 0),
    TotalAmount DECIMAL(18,2) NOT NULL CHECK (TotalAmount >= 0),
    OrderStatus VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    PaymentMethod VARCHAR(20) NOT NULL DEFAULT 'COD',
    PaymentStatus VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    Note NVARCHAR(500) NULL,
    CONSTRAINT FK_Orders_Users FOREIGN KEY (UserId) REFERENCES Users(UserId)
);

CREATE TABLE OrderItems (
    OrderItemId BIGINT IDENTITY(1,1) PRIMARY KEY,
    OrderId BIGINT NOT NULL,
    VariantId INT NOT NULL,
    Quantity INT NOT NULL CHECK (Quantity > 0),
    UnitPrice DECIMAL(18,2) NOT NULL CHECK (UnitPrice >= 0),
    LineTotal DECIMAL(18,2) NOT NULL CHECK (LineTotal >= 0),
    CONSTRAINT FK_OrderItems_Orders FOREIGN KEY (OrderId) REFERENCES Orders(OrderId),
    CONSTRAINT FK_OrderItems_ProductVariants FOREIGN KEY (VariantId) REFERENCES ProductVariants(VariantId)
);

CREATE TABLE Carts (
    CartId BIGINT IDENTITY(1,1) PRIMARY KEY,
    UserId INT NOT NULL UNIQUE,
    UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Carts_Users FOREIGN KEY (UserId) REFERENCES Users(UserId)
);

CREATE TABLE CartItems (
    CartItemId BIGINT IDENTITY(1,1) PRIMARY KEY,
    CartId BIGINT NOT NULL,
    VariantId INT NOT NULL,
    Quantity INT NOT NULL CHECK (Quantity > 0),
    CONSTRAINT FK_CartItems_Carts FOREIGN KEY (CartId) REFERENCES Carts(CartId),
    CONSTRAINT FK_CartItems_ProductVariants FOREIGN KEY (VariantId) REFERENCES ProductVariants(VariantId),
    CONSTRAINT UQ_CartItems_Cart_Variant UNIQUE (CartId, VariantId)
);

INSERT INTO Roles (RoleName)
VALUES (N'ADMIN'), (N'STAFF'), (N'CUSTOMER');

INSERT INTO Categories (CategoryName, Description)
VALUES
(N'Áo thun', N'Áo thun nam nữ'),
(N'Áo sơ mi', N'Áo sơ mi'),
(N'Quần jean', N'Quần jean'),
(N'Áo khoác', N'Áo khoác');

CREATE INDEX IX_Products_CategoryId ON Products(CategoryId);
CREATE INDEX IX_ProductVariants_ProductId ON ProductVariants(ProductId);
CREATE INDEX IX_Orders_UserId_OrderDate ON Orders(UserId, OrderDate DESC);
CREATE INDEX IX_Orders_OrderStatus ON Orders(OrderStatus);
CREATE INDEX IX_OrderItems_OrderId ON OrderItems(OrderId);
GO
```

**Lưu ý quan trọng về dữ liệu mẫu:** script trên chỉ khởi tạo schema và
một ít dữ liệu danh mục/vai trò để bắt đầu. Nó **chưa đáp ứng yêu cầu 20
bản ghi cho mỗi bảng chính**. Nhóm phải bổ sung dữ liệu mẫu cho Users,
Products, ProductVariants, Orders và OrderItems; dữ liệu mẫu cần có liên
kết khóa ngoại hợp lệ. Không chèn mật khẩu dạng văn bản thuần cho tài
khoản demo; hãy tạo hash bằng ứng dụng hoặc seed an toàn.

## 7. Phân quyền đề xuất

### Vai trò

  Chức năng                     Khách chưa đăng nhập             CUSTOMER                STAFF   ADMIN
  --------------------------- ---------------------- -------------------- -------------------- -------
  Xem/tìm sản phẩm                                Có                   Có                   Có      Có
  Thêm vào giỏ/đặt hàng                        Không                   Có      Theo chính sách      Có
  Xem đơn hàng của mình                        Không                   Có   Có quyền xử lý đơn      Có
  Xem tất cả đơn                               Không                Không                   Có      Có
  Cập nhật trạng thái đơn                      Không   Chỉ hủy nếu hợp lệ                   Có      Có
  CRUD sản phẩm/danh mục                       Không                Không       Có thể chỉ xem      Có
  Điều chỉnh tồn kho                           Không                Không       Theo phân công      Có
  Quản lý tài khoản/vai trò                    Không   Chỉ thông tin mình                Không      Có
  Xem báo cáo quản trị                         Không                Không           Theo quyền      Có

### Cách triển khai

-   Dùng Spring Security để đăng nhập, đăng xuất và bảo vệ URL.
-   `PasswordEncoder` để hash mật khẩu.
-   Dùng các vai trò như `ROLE_ADMIN`, `ROLE_STAFF`, `ROLE_CUSTOMER`
    nhất quán trong cấu hình.
-   Ví dụ: `/admin/**` chỉ Admin; `/staff/**` cho Staff/Admin;
    `/orders/my/**` yêu cầu đăng nhập.
-   Kiểm tra quyền ở backend cho từng đơn hàng: Customer chỉ xem đơn
    thuộc `UserId` của chính mình.
-   Ẩn menu theo vai trò là trải nghiệm giao diện, không thay thế bảo
    mật backend.
-   Không cho phép đăng ký công khai vai trò Admin/Staff. Tài khoản quản
    trị được seed an toàn hoặc do Admin tạo.

## 8. Sườn source code Spring Boot

``` text
clothing-shop/
├── pom.xml
├── README.md
├── database/
│   ├── 01_create_schema.sql
│   ├── 02_seed_roles_categories.sql
│   └── 03_seed_demo_data.sql
├── src/main/java/com/shop/clothing/
│   ├── ClothingShopApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   └── AsyncConfig.java
│   ├── entity/
│   │   ├── Role.java
│   │   ├── User.java
│   │   ├── Category.java
│   │   ├── Product.java
│   │   ├── ProductVariant.java
│   │   ├── Order.java
│   │   ├── OrderItem.java
│   │   ├── Cart.java
│   │   └── CartItem.java
│   ├── repository/
│   ├── service/
│   ├── service/impl/
│   ├── controller/
│   ├── dto/
│   ├── validation/
│   ├── exception/
│   ├── network/
│   │   ├── ShopSocketServer.java
│   │   ├── ClientHandler.java
│   │   └── ShopSocketClient.java
│   └── concurrency/
│       └── InventoryService.java
├── src/main/resources/
│   ├── application.properties
│   ├── templates/
│   │   ├── fragments/
│   │   ├── auth/
│   │   ├── shop/
│   │   ├── cart/
│   │   ├── orders/
│   │   └── admin/
│   └── static/
│       ├── css/
│       ├── js/
│       └── images/
└── src/test/java/com/shop/clothing/
    ├── service/
    ├── repository/
    ├── concurrency/
    └── network/
```

Đây là sườn tham khảo; nhóm có thể đổi package nhưng phải thống nhất
trước. Nếu `service/impl` không cần thiết thì có thể để implementation
ngay trong `service`. Socket Client có thể được đặt trong một
module/project riêng để chứng minh là client tách biệt.

## 9. Quy định chung: phải thống nhất trước khi chia code

### 9.1 Chốt hợp đồng chung

Trước khi mỗi người làm riêng, nhóm cần thống nhất và commit các nội
dung sau:

1.  Tên project, package gốc, JDK và phiên bản Spring Boot.
2.  Tên bảng/cột, kiểu dữ liệu, khóa và quan hệ database.
3.  Tên Entity, Repository, Service và DTO.
4.  Quy ước tên method và đường dẫn URL.
5.  Quy ước trả lỗi, thông báo và validation.
6.  Vai trò và quy tắc phân quyền.
7.  Quy trình trạng thái đơn hàng và quy tắc cập nhật kho.
8.  Cách tích hợp Socket, định dạng request/response, cổng server.
9.  Quy tắc Git branch, pull request/merge và xử lý conflict.
10. Danh sách chức năng và người phụ trách.

### 9.2 Cấu hình database và môi trường

-   Một thành viên tạo database và script chuẩn; cả nhóm dùng cùng
    schema.
-   Mỗi thành viên cấu hình kết nối SQL Server trên máy mình bằng biến
    môi trường hoặc file local không commit.
-   Không để mật khẩu thật trong Git.
-   Không tự ý đổi tên cột hoặc quan hệ sau khi người khác đã tạo
    Entity/Repository; nếu phải đổi, cập nhật script và báo cả nhóm.
-   Chốt dữ liệu mẫu dùng chung để mọi người test cùng một kết quả.

### 9.3 Quy tắc code

-   Controller không chứa toàn bộ nghiệp vụ hoặc truy vấn database trực
    tiếp.
-   Service xử lý nghiệp vụ; dùng `@Transactional` cho thao tác nhiều
    bước phù hợp.
-   Repository chịu trách nhiệm truy cập dữ liệu.
-   Form nhận dữ liệu nên dùng DTO khi phù hợp; không bind tùy tiện các
    trường nhạy cảm như role, password hash, trạng thái thanh toán.
-   Validate ở backend; JavaScript validation chỉ hỗ trợ UX.
-   Mọi chức năng phải có trạng thái thành công/thất bại rõ ràng.
-   Mỗi thành viên phải tạo commit nhỏ, có ý nghĩa; không dồn toàn bộ
    code vào một commit cuối.
-   Mỗi chức năng phải có bước tự test trước khi bàn giao.

### 9.4 Git workflow

1.  Người quản lý repository tạo repo và nhánh `main`.
2.  Mỗi thành viên tạo nhánh riêng, ví dụ `feature/product-management`.
3.  Kéo thay đổi mới nhất trước khi code và trước khi merge.
4.  Tạo pull request hoặc merge request, ít nhất một thành viên khác
    kiểm tra.
5.  Chỉ merge code đã build/test được.
6.  Không commit `target/`, file IDE cá nhân, mật khẩu, file log hay
    thông tin kết nối thật.
7.  Lưu lịch sử commit phản ánh đúng phần đóng góp của từng người.

## 10. Chia việc cụ thể cho 5 thành viên và thứ tự bàn giao

Để hạn chế xung đột, thứ tự dưới đây là **thứ tự bàn giao tích hợp**,
không có nghĩa người sau phải chờ hoàn toàn người trước mới bắt đầu. Sau
khi có hợp đồng chung, các thành viên có thể code song song bằng
interface/DTO đã thống nhất.

### Thành viên 1 --- Trưởng nhóm, Database, Entity và Repository

**Trách nhiệm chính:** nền tảng dữ liệu và tích hợp.

Các việc phải làm:

1.  Tạo repository Git và project Spring Boot khung.
2.  Chốt package, phiên bản JDK/Spring Boot, dependencies.
3.  Viết ERD và data dictionary.
4.  Viết script tạo 9 bảng, khóa, index và ràng buộc.
5.  Bổ sung dữ liệu mẫu hợp lệ, tối thiểu 20 bản ghi cho mỗi bảng chính
    theo đề.
6.  Tạo các Entity: `Role`, `User`, `Category`, `Product`,
    `ProductVariant`, `Order`, `OrderItem`, `Cart`, `CartItem`.
7.  Ánh xạ `@ManyToOne`, `@OneToMany`, `@OneToOne` nếu phù hợp; tránh
    vòng lặp JSON/view không cần thiết.
8.  Tạo Repository cho từng Entity.
9.  Viết ít nhất 2 truy vấn tùy chỉnh, ví dụ tìm kiếm sản phẩm theo từ
    khóa/danh mục/khoảng giá và thống kê doanh thu theo thời gian.
10. Cấu hình kết nối SQL Server và xác minh script chạy trên database
    sạch.
11. Viết README phần cài đặt database.
12. Review code và giải quyết tích hợp với thành viên khác.

**Bàn giao theo thứ tự:** - Đợt 1: `pom.xml`, cấu trúc package, script
SQL, ERD và quy ước tên. - Đợt 2: Entity, Repository, cấu hình kết nối
và dữ liệu mẫu. - Đợt 3: truy vấn tùy chỉnh, kiểm tra ràng buộc, README
database.

**Tiêu chí rubric chính:** 1.3, 1.4 (phối hợp), 2.1, 4.1, 4.2, 4.3 và
một phần 6.4.

### Thành viên 2 --- Đăng nhập, phân quyền, tài khoản và hồ sơ khách hàng

**Trách nhiệm chính:** xác thực và bảo mật.

Các việc phải làm:

1.  Tạo `SecurityConfig`.
2.  Cấu hình trang đăng nhập, đăng xuất và xử lý người dùng chưa đăng
    nhập.
3.  Tạo Service xác thực và dùng `PasswordEncoder`.
4.  Tạo trang đăng ký Customer, kiểm tra email trùng và dữ liệu đầu vào.
5.  Không cho khách tự chọn vai trò Admin/Staff khi đăng ký.
6.  Tạo chức năng xem/sửa hồ sơ cá nhân và đổi mật khẩu.
7.  Tạo cơ chế seed tài khoản Admin an toàn, không commit mật khẩu thật.
8.  Cấu hình phân quyền URL theo vai trò.
9.  Kiểm tra Customer không xem được đơn hàng của người khác.
10. Tạo thông báo đăng nhập sai, tài khoản bị khóa và truy cập không đủ
    quyền.
11. Viết test cho đăng nhập, phân quyền và trường hợp truy cập trái
    phép.
12. Phối hợp thành viên 3 để menu hiển thị theo vai trò.

**Bàn giao theo thứ tự:** - Đợt 1: quy ước role và SecurityConfig tối
thiểu. - Đợt 2: đăng nhập/đăng xuất, đăng ký và hồ sơ. - Đợt 3: bảo vệ
URL, kiểm thử quyền và hướng dẫn tài khoản demo.

**Tiêu chí rubric chính:** 2.2, 2.5, 3.3, 3.5 và 6.3.

### Thành viên 3 --- Sản phẩm, danh mục, biến thể, giao diện cửa hàng

**Trách nhiệm chính:** danh mục và trải nghiệm xem/mua sản phẩm.

Các việc phải làm:

1.  CRUD danh mục cho Admin.
2.  CRUD sản phẩm cho Admin.
3.  Quản lý biến thể size/màu/SKU/tồn kho ban đầu.
4.  Không cho SKU trùng, giá âm hoặc tồn kho âm.
5.  Trang chủ và trang danh sách sản phẩm.
6.  Trang chi tiết sản phẩm, hiển thị ảnh, mô tả, giá, size, màu và tồn
    kho.
7.  Tìm kiếm theo tên/mã; lọc danh mục, size, màu và giá.
8.  Phân trang và sắp xếp.
9.  Dùng Thymeleaf fragments cho header, menu, footer.
10. Dùng Bootstrap, kiểm tra trên màn hình điện thoại.
11. Thêm thông báo lỗi/thành công và xác nhận xóa.
12. Không xóa cứng sản phẩm/biến thể đang được đơn hàng tham chiếu; xử
    lý bằng `IsActive` hoặc thông báo phù hợp.
13. Phối hợp thành viên 1 để dùng đúng tên Entity/Repository và thành
    viên 2 để phân quyền trang quản trị.

**Bàn giao theo thứ tự:** - Đợt 1: layout, fragments, giao diện danh
sách. - Đợt 2: CRUD danh mục/sản phẩm/biến thể. - Đợt 3: tìm kiếm, lọc,
phân trang, responsive và thông báo.

**Tiêu chí rubric chính:** 2.2, 2.4, 3.1--3.5, 4.4.

### Thành viên 4 --- Giỏ hàng, đặt hàng, xử lý đơn và tồn kho

**Trách nhiệm chính:** nghiệp vụ bán hàng cốt lõi và transaction.

Các việc phải làm:

1.  Thiết kế `CartService` và `OrderService`.
2.  Thêm biến thể vào giỏ; cập nhật/xóa số lượng.
3.  Kiểm tra tồn kho khi thêm giỏ và khi checkout.
4.  Trang giỏ hàng và form checkout.
5.  Lấy giá từ database, tính tổng tiền ở server.
6.  Tạo `Order` và `OrderItems`, lưu giá tại thời điểm mua.
7.  Trong một transaction, kiểm tra/cập nhật tồn kho và tạo đơn để tránh
    cập nhật dở dang.
8.  Xử lý hai yêu cầu mua đồng thời khi tồn kho thấp; dùng locking hoặc
    cập nhật có điều kiện phù hợp.
9.  Xóa giỏ hoặc cập nhật giỏ sau khi đặt hàng thành công.
10. Trang lịch sử đơn và chi tiết đơn của Customer.
11. Cho phép hủy đơn theo trạng thái/điều kiện đã thống nhất.
12. Tạo màn hình Staff/Admin xác nhận đơn và cập nhật trạng thái.
13. Ngăn khách sửa trạng thái đơn hoặc xem đơn người khác.
14. Viết test cho checkout, tồn kho không âm, trùng yêu cầu và rollback
    khi lỗi.

**Bàn giao theo thứ tự:** - Đợt 1: DTO và hợp đồng nghiệp vụ checkout. -
Đợt 2: giỏ hàng, checkout, đơn hàng và transaction. - Đợt 3: quản lý
trạng thái, test tồn kho và hướng dẫn demo.

**Tiêu chí rubric chính:** 2.3, 2.4, 4.4, 4.5 và phối hợp tiêu chí
5.1--5.2.

### Thành viên 5 --- Đa luồng, Socket Client/Server, kiểm thử và hỗ trợ báo cáo

**Trách nhiệm chính:** đáp ứng phần mạng và đồng thời; không làm một
demo không liên quan nghiệp vụ.

Các việc phải làm:

1.  Viết tài liệu giao thức: loại request, trường dữ liệu, phản hồi
    thành công/lỗi.
2.  Viết `ShopSocketServer` sử dụng `ServerSocket`.
3.  Dùng `ExecutorService` hoặc mô hình thread-per-client có giới hạn để
    xử lý nhiều kết nối.
4.  Viết `ClientHandler` xử lý từng client.
5.  Viết chương trình `ShopSocketClient` tách biệt, có thể chạy nhiều
    bản.
6.  Định nghĩa lệnh nghiệp vụ liên quan shop, ví dụ `PING`,
    `CHECK_STOCK`, `GET_ORDER_STATUS` hoặc yêu cầu kiểm tra kho.
7.  Không nhận dữ liệu client một cách mù quáng; validate request, giới
    hạn kích thước và xử lý dữ liệu sai.
8.  Đảm bảo server phục vụ ít nhất 3 client đồng thời.
9.  Xử lý client mất kết nối, timeout phù hợp, đóng
    socket/stream/executor đúng cách.
10. Phối hợp thành viên 4 để thiết kế bài test nhiều luồng kiểm tra tồn
    kho.
11. Viết test chạy nhiều luồng cùng gửi yêu cầu và chứng minh kết quả
    không vượt tồn kho.
12. Ghi log có thời điểm, client/thread và kết quả để làm minh chứng.
13. Viết phần kiểm thử đa luồng/mạng cho Chương 4.
14. Chuẩn bị kịch bản demo 3 client và hướng dẫn chạy server/client.

**Bàn giao theo thứ tự:** - Đợt 1: giao thức, sơ đồ và skeleton
server/client. - Đợt 2: server phục vụ nhiều client và xử lý lỗi. - Đợt
3: tích hợp nghiệp vụ kiểm tra kho/đơn, test đồng thời, log và kịch bản
demo.

**Tiêu chí rubric chính:** 1.5, 5.1, 5.2, 5.3, 5.4, 6.1 (phối hợp).

## 11. Thứ tự code và nộp bài theo từng đợt

  -----------------------------------------------------------------------
  Đợt                     Người bàn giao chính    Những gì phải chạy/được
                                                  duyệt
  ----------------------- ----------------------- -----------------------
  0 --- Thống nhất        Cả nhóm, trưởng nhóm    Đề tài, chức năng, ERD
                          chốt                    nháp, package, role,
                                                  quy tắc Git

  1 --- Nền móng          Thành viên 1            Project build được,
                                                  database/script,
                                                  Entity/Repository ban
                                                  đầu

  2 --- Khung chạy        Thành viên 2 + 3        Đăng nhập cơ bản,
                                                  layout Thymeleaf, trang
                                                  danh sách sản phẩm

  3 --- CRUD              Thành viên 1 + 3        CRUD danh mục/sản
                                                  phẩm/biến thể, dữ liệu
                                                  lưu thật trong SQL
                                                  Server

  4 --- Bán hàng          Thành viên 4            Giỏ hàng, checkout, đơn
                                                  hàng, cập nhật tồn kho
                                                  và transaction

  5 --- Phân quyền        Thành viên 2 + 4        Customer/Staff/Admin
                                                  đúng quyền, không lộ
                                                  đơn hàng người khác

  6 --- Mạng/đa luồng     Thành viên 5 + 4        Server/client, 3
                                                  client, test đồng thời
                                                  và log

  7 --- Hoàn thiện        Cả nhóm                 Tìm kiếm, phân trang,
                                                  thông báo, lỗi,
                                                  responsive, test tích
                                                  hợp

  8 --- Hồ sơ nộp         Trưởng nhóm + cả nhóm   Báo cáo, slide, README,
                                                  script SQL, bảng phân
                                                  công, Git, video demo
  -----------------------------------------------------------------------

### Quy tắc bàn giao code

Mỗi lần bàn giao, người phụ trách phải gửi: - Danh sách file đã
thêm/sửa. - Chức năng đã hoàn thành. - Cách chạy và dữ liệu test. -
Ảnh/video hoặc kết quả test. - Các lỗi còn tồn tại. - Commit ID/nhánh
Git.

Không coi "đã code xong" là hoàn thành nếu code chưa build, chưa chạy
với database chung hoặc chưa có người khác kiểm tra.

## 12. Đa luồng và mạng: cách gắn với shop quần áo

### Tình huống đa luồng nên dùng

Giả sử biến thể áo thun SKU `AT-DEN-M` chỉ còn 2 chiếc. Ba yêu cầu đặt
hàng cùng lúc đều muốn mua 1 chiếc.

Kết quả hợp lệ: chỉ tối đa 2 chiếc được bán; yêu cầu còn lại phải báo
hết hàng hoặc không đủ tồn kho. Không được để tồn kho thành -1, -2 hoặc
tạo 3 chiếc bán từ kho có 2 chiếc.

Cách triển khai: - Kiểm tra tồn kho ở server, không tin số lượng từ
trình duyệt. - Dùng transaction và cơ chế khóa/cập nhật có điều kiện ở
database phù hợp với SQL Server. - Kiểm thử nhiều luồng đồng thời; không
chỉ dùng `Thread.sleep()` để giả vờ có đồng thời. - Ghi kết quả mong đợi
và kết quả thực tế.

### Tình huống mạng nên demo

-   Khởi chạy Socket Server.
-   Mở 3 chương trình Client riêng.
-   Mỗi client gửi yêu cầu `CHECK_STOCK` cho SKU đã biết.
-   Server xử lý đồng thời, trả về số lượng hoặc trạng thái.
-   Kiểm tra server vẫn hoạt động khi một client nhập sai hoặc đóng đột
    ngột.

Socket Server cần gắn với bài toán shop và có giao thức rõ ràng. Nếu
server chỉ trả lời "Hello" thì tuy có thể minh họa kết nối, nhưng bằng
chứng nghiệp vụ sẽ yếu hơn.

## 13. Kiểm thử bắt buộc trước khi nộp

  -----------------------------------------------------------------------
  Nhóm test               Kịch bản                Kết quả mong đợi
  ----------------------- ----------------------- -----------------------
  Đăng nhập               Sai mật khẩu            Báo lỗi, không đăng
                                                  nhập

  Phân quyền              Customer truy cập       Bị từ chối/chuyển trang
                          `/admin/**`             phù hợp

  Quyền dữ liệu           Customer xem đơn của    Không được xem
                          user khác               

  Sản phẩm                Nhập giá âm hoặc SKU    Validation/ràng buộc
                          trùng                   chặn

  CRUD                    Xóa danh mục đang được  Báo lỗi hoặc vô hiệu
                          sản phẩm sử dụng        hóa an toàn

  Tìm kiếm                Tìm từ khóa và lọc danh Kết quả đúng, phân
                          mục                     trang hoạt động

  Checkout                Số lượng lớn hơn tồn    Không tạo đơn sai,
                          kho                     không trừ kho

  Transaction             Có lỗi giữa quá trình   Rollback dữ liệu liên
                          checkout                quan

  Đồng thời               Nhiều yêu cầu mua hơn   Không bán vượt tồn kho
                          tồn kho                 

  Socket                  3 client kết nối cùng   Cả 3 nhận phản hồi
                          lúc                     

  Socket lỗi              Client ngắt đột ngột    Server không chết, tài
                                                  nguyên được đóng

  Cài đặt sạch            Chạy trên máy khác với  Có thể dựng database và
                          script/README           chạy ứng dụng
  -----------------------------------------------------------------------

## 14. Cấu trúc báo cáo và sản phẩm nộp

### Báo cáo 25--40 trang

1.  **Chương 1 --- Tổng quan:** lý do chọn đề tài, bài toán, mục tiêu,
    phạm vi, công cụ.
2.  **Chương 2 --- Phân tích và thiết kế:** vai trò, tối thiểu 10 chức
    năng, use case, ERD, data dictionary, kiến trúc Spring Boot, thiết
    kế đa luồng và mạng.
3.  **Chương 3 --- Cài đặt:** cấu trúc source, cấu hình,
    Entity/Repository/Service/Controller tiêu biểu, ảnh giao diện và
    giải thích code.
4.  **Chương 4 --- Kiểm thử:** test chức năng, database, phân quyền,
    transaction, nhiều luồng, 3 client; kết quả và ảnh minh chứng.
5.  **Chương 5 --- Kết luận:** kết quả, hạn chế, hướng phát triển.
6.  Tài liệu tham khảo và phụ lục phân công.

### Danh sách sản phẩm phải nộp

-   [ ] Link GitHub/GitLab và lịch sử commit.
-   [ ] Source code đầy đủ.
-   [ ] Script SQL tạo bảng và dữ liệu mẫu.
-   [ ] README hướng dẫn JDK, SQL Server, cấu hình và chạy.
-   [ ] Báo cáo PDF/Word 25--40 trang.
-   [ ] Slide tối đa 20 trang.
-   [ ] Bảng phân công và xác nhận nhóm.
-   [ ] Video demo 5--8 phút nếu có thể.
-   [ ] Kịch bản demo trực tiếp: website, CRUD, đặt hàng, tồn kho, đa
    luồng và 3 client.

## 15. Checklist khởi động trong buổi đầu tiên

-   [ ] Chốt tên project và tên database `ClothingShopDB`.
-   [ ] Chốt 9 bảng và quan hệ trước khi code Entity.
-   [ ] Chốt 3 vai trò `ADMIN`, `STAFF`, `CUSTOMER`.
-   [ ] Chốt danh sách chức năng tối thiểu.
-   [ ] Chốt nghiệp vụ đa luồng: đặt hàng/tồn kho.
-   [ ] Chốt Socket Server phục vụ truy vấn nghiệp vụ shop.
-   [ ] Tạo repository Git, `main` và nhánh từng thành viên.
-   [ ] Tạo project Spring Boot và kiểm tra build.
-   [ ] Commit schema SQL, ERD và README ban đầu.
-   [ ] Chia issue/task theo 5 thành viên, có người phụ trách và điều
    kiện hoàn thành.

**Nguyên tắc cuối cùng:** ưu tiên ứng dụng chạy ổn định và đáp ứng đủ
rubric trước; sau đó mới làm tính năng nâng cao như mã giảm giá, đánh
giá sản phẩm, email, thanh toán trực tuyến hoặc biểu đồ đẹp. Không hy
sinh đa luồng, lập trình mạng, dữ liệu mẫu, báo cáo và khả năng demo để
đổi lấy quá nhiều chức năng phụ.
