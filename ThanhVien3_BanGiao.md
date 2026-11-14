# Bàn giao Thành viên 3: Sản phẩm, Danh mục, Biến thể và Giao diện cửa hàng

## 1. File đã thêm

Đường dẫn tính từ thư mục gốc của repo (cùng cấp với `pom.xml`).

**Controller (Quản trị & Cửa hàng)**

| File | Vai trò |
|---|---|
| `src/main/java/com/example/clothingshop/controller/CategoryController.java` | Quản lý danh mục cho Admin (`/admin/categories`): thêm, xem, sửa, ẩn/vô hiệu hóa danh mục |
| `src/main/java/com/example/clothingshop/controller/ProductController.java` | Quản lý sản phẩm cho Admin (`/admin/products`): CRUD sản phẩm, tìm kiếm, lọc, ngừng kinh doanh (`IsActive = false`) |
| `src/main/java/com/example/clothingshop/controller/ProductVariantController.java` | Quản lý biến thể sản phẩm (`/admin/variants`): thêm/quản lý size, màu sắc, mã SKU, tồn kho ban đầu |
| `src/main/java/com/example/clothingshop/controller/ProductShopController.java` | Hiển thị danh sách sản phẩm phía khách hàng (`/products`), xử lý tìm kiếm, lọc, phân trang và sắp xếp |
| `src/main/java/com/example/clothingshop/controller/ProductDetailController.java` | Hiển thị trang chi tiết sản phẩm (`/products/{id}`), danh sách biến thể size/màu/tồn kho |

**Giao diện Quản trị (Thymeleaf - Admin)**

| File | Vai trò |
|---|---|
| `src/main/resources/templates/admin/categories.html` | Danh sách danh mục quản trị, trạng thái hoạt động và nút thao tác |
| `src/main/resources/templates/admin/categories-form.html` | Form thêm mới danh mục kèm thông báo validate dữ liệu |
| `src/main/resources/templates/admin/categories-edit.html` | Form cập nhật thông tin và trạng thái danh mục |
| `src/main/resources/templates/admin/products.html` | Danh sách sản phẩm quản trị, bộ lọc tìm kiếm, nút sửa, quản lý biến thể và ngừng kinh doanh |
| `src/main/resources/templates/admin/product-form.html` | Form thêm mới sản phẩm, kiểm tra trùng tên, giá > 0 và danh mục hợp lệ |
| `src/main/resources/templates/admin/product-edit.html` | Form cập nhật thông tin sản phẩm, hình ảnh và trạng thái kinh doanh |
| `src/main/resources/templates/admin/variants.html` | Danh sách biến thể (Size, Màu sắc, SKU, Tồn kho) theo từng sản phẩm |
| `src/main/resources/templates/admin/variant-form.html` | Form thêm biến thể mới, chặn trùng SKU, chặn trùng `(ProductId, Size, Color)` và tồn kho âm |

**Giao diện Cửa hàng & Tài nguyên tĩnh**

| File | Vai trò |
|---|---|
| `src/main/resources/templates/products.html` | Trang danh sách sản phẩm khách hàng: tìm kiếm theo từ khóa, lọc danh mục/size/màu/khoảng giá, phân trang & sắp xếp |
| `src/main/resources/templates/product-detail.html` | Trang chi tiết sản phẩm: hình ảnh, mô tả, giá bán, bảng biến thể size/màu/tồn kho |
| `src/main/resources/templates/index.html` | Trang chủ cửa hàng với banner slider 3 ảnh (Bootstrap 5) |
| `src/main/resources/templates/fragments/header.html`, `menu.html`, `footer.html` | Các mảnh giao diện dùng chung (Thymeleaf fragments) đồng bộ toàn bộ website |
| `src/main/resources/static/images/*` | 24 ảnh sản phẩm khớp dữ liệu mẫu SQL và 3 ảnh banner (`banner-1.jpg`, `banner-2.jpg`, `banner-3.jpg`) |

## 2. File đã sửa (bổ sung truy vấn & tích hợp nhóm)

| File | Thay đổi |
|---|---|
| `src/main/java/com/example/clothingshop/repository/CategoryRepository.java` | Bổ sung các phương thức truy vấn danh mục đang hoạt động và kiểm tra dữ liệu trùng/ràng buộc |
| `src/main/java/com/example/clothingshop/repository/ProductRepository.java` | Bổ sung `@Query` tìm kiếm theo từ khóa, lọc theo danh mục & khoảng giá (`searchProducts`), kiểm tra trùng tên (`existsByProductNameIgnoreCase`) |
| `src/main/java/com/example/clothingshop/repository/ProductVariantRepository.java` | Bổ sung các câu lệnh kiểm tra trùng mã `SKU`, trùng tổ hợp size/màu và lấy danh sách biến thể theo sản phẩm |

## 3. Chức năng đã hoàn thành (đối chiếu 13 việc trong kế hoạch)

| # | Việc | Trạng thái |
|---|---|---|
| 1 | CRUD danh mục cho Admin | Xong (`CategoryController` + `categories*.html`) |
| 2 | CRUD sản phẩm cho Admin | Xong (`ProductController` + `products.html`, `product-form.html`, `product-edit.html`) |
| 3 | Quản lý biến thể size/màu/SKU/tồn kho ban đầu | Xong (`ProductVariantController` + `variants.html`, `variant-form.html`) |
| 4 | Không cho SKU trùng, giá âm hoặc tồn kho âm | Xong (validate cả phía Controller/Repository lẫn ràng buộc dữ liệu) |
| 5 | Trang chủ và trang danh sách sản phẩm | Xong (`index.html`, `products.html`, `ProductShopController`) |
| 6 | Trang chi tiết sản phẩm (ảnh, mô tả, giá, size, màu, tồn kho) | Xong (`product-detail.html`, `ProductDetailController`) |
| 7 | Tìm kiếm theo tên/mã; lọc danh mục, size, màu và giá | Xong |
| 8 | Phân trang và sắp xếp | Xong |
| 9 | Dùng Thymeleaf fragments cho header, menu, footer | Xong (`templates/fragments/*`) |
| 10 | Dùng Bootstrap 5, kiểm tra trên màn hình điện thoại | Xong |
| 11 | Thêm thông báo lỗi/thành công và xác nhận xóa | Xong (`RedirectAttributes` + `confirm` trước khi ngừng bán/hủy) |
| 12 | Không xóa cứng sản phẩm/biến thể đang được đơn hàng tham chiếu | Xong (xử lý xóa mềm chuyển `IsActive = false` / Ngừng kinh doanh) |
| 13 | Phối hợp TV1 (Entity/Repository) và TV2 (phân quyền trang quản trị) | Cần TV1 và TV2 kiểm tra lại |

## 3b. Dữ liệu test dùng để demo

Dùng dữ liệu mẫu của `database/ClothingShopDB.sql` (24 sản phẩm, ảnh lưu tại `src/main/resources/static/images/`):
- **Sản phẩm & SKU mẫu:** `Áo thun basic trắng` (`SKU-001-M-BLK`), `Thắt lưng da tổng hợp`, `Quần jean slim`...
- **Ca test Validate (nhập sai):** Nhập giá `<= 0` (ví dụ `-50000`), tồn kho `< 0`, tên sản phẩm trùng hoặc mã `SKU` đã tồn tại $\rightarrow$ hệ thống chặn lưu và hiển thị thông báo lỗi trên form.
- **Ca test Xóa mềm:** Bấm **"Ngừng kinh doanh"** ở `/admin/products` $\rightarrow$ cập nhật `IsActive = false`, sản phẩm ẩn khỏi trang mua hàng nhưng vẫn giữ nguyên dữ liệu lịch sử của các đơn hàng cũ.

## 4. Cách chạy

```text
# 1. Khởi tạo database ClothingShopDB và chạy script dữ liệu mẫu trên SQL Server
# 2. Chạy ứng dụng Spring Boot (cổng 8081)
mvnw spring-boot:run          # hoặc chạy ClothingShopApplication trong IntelliJ IDEA

# 3. Các đường dẫn kiểm tra chính trên trình duyệt:
# - Trang chủ & Banner slider:     http://localhost:8081/
# - Danh sách & Lọc sản phẩm:      http://localhost:8081/products
# - Chi tiết:                      http://localhost:8081/products/1
# - Quản trị Sản phẩm (Admin):     http://localhost:8081/admin/products
# - Quản trị Danh mục (Admin):     http://localhost:8081/admin/categories
# - Quản trị Biến thể (Admin):     http://localhost:8081/admin/variants
```

## 5. Kết quả đã kiểm tra và bằng chứng

**Đã chạy và đạt**
- Toàn bộ 24 ảnh sản phẩm và 3 ảnh banner trượt tự động ở trang chủ (`http://localhost:8081/`) hiển thị đúng kích thước, không vỡ ảnh.
- Các chức năng CRUD ở `CategoryController`, `ProductController`, `ProductVariantController` lưu dữ liệu thật xuống SQL Server và hiển thị thông báo thành công/thất bại rõ ràng.
- Luồng tìm kiếm tiếng Việt có dấu (như `"thắt lưng"`, `"áo thun"`), lọc theo danh mục/giá/size/màu, phân trang và sắp xếp chạy ổn định.
- Đã kiểm tra hiển thị responsive trên kích thước màn hình điện thoại (F12 DevTools).

**Chưa chạy / Cần nhóm kiểm tra thêm**
- Nhờ Thành viên 2 và Thành viên 4 test chéo lại luồng phân quyền `/admin/**` và luồng checkout hoàn chỉnh sau khi merge vào `main`.