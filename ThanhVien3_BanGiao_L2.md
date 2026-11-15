# Bàn giao Thành viên 3 (Đợt bổ sung - Deadline 2): Đồng bộ Menu Phân Quyền, Mua Nhanh & Upload Ảnh Sản Phẩm

Bàn giao theo quy tắc bàn giao code của nhóm cho đợt hoàn thiện giao diện và trải nghiệm người dùng.

Nhánh Git: `feature/tv3-deadline2`

## 1. File đã thêm

| File | Vai trò |
|---|---|
| `src/main/java/com/example/clothingshop/controller/GlobalMenuAdvice.java` | `@ControllerAdvice` tự động gửi biến `currentPath` (`request.getRequestURI()`) sang toàn bộ giao diện Thymeleaf để tô sáng tab menu đang mở |

## 2. File đã sửa

| File | Thay đổi                                                                                                                                                                                                                 |
|---|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `src/main/resources/templates/fragments/bootstrap-menu.html` | Chuẩn hóa thanh menu Bootstrap 5 (`CLOTHING SHOP`), tự động tô sáng (active nền vàng) tab tương ứng trang đang mở; phân tách hiển thị theo vai trò (`CUSTOMER`, `STAFF`, `ADMIN`) và ẩn Giỏ hàng đối với `ADMIN`/`STAFF` |
| `src/main/resources/templates/fragments/admin-menu.html` | Đồng bộ kiểu active tab nền vàng cho thanh menu quản trị (`CLOTHING ADMIN`), bổ sung mục **Đơn hàng** (`/staff/orders`) và nút **Về cửa hàng**                                                                           |
| `src/main/resources/templates/fragments/menu.html` | Chuyển sang dùng chung fragment `fragments/bootstrap-menu :: menu` để đồng bộ giao diện toàn website                                                                                                                     |
| `src/main/resources/templates/fragments/account-controls.html` | Bổ sung biểu tượng tài khoản (SVG `bi-person-circle`) cạnh tên và vai trò người dùng đang đăng nhập                                                                                                                      |
| `src/main/resources/templates/admin/index.html` | Bổ sung thẻ truy cập nhanh **Quản lý đơn hàng** (`/staff/orders`) trên trang Tổng quan hệ thống của Admin                                                                                                                |
| `src/main/resources/templates/index.html` | Gắn nút **Thêm vào giỏ** + **Mua ngay** trên thẻ sản phẩm (ẩn với `ADMIN`/`STAFF`)                                                                                                                                       |
| `src/main/resources/templates/products.html` | Bổ sung nút **Thêm vào giỏ** (`POST /cart/add`) và **Mua ngay** (`/products/{id}?buyNow=true`) trực tiếp trên thẻ sản phẩm (ẩn với `ADMIN`/`STAFF`)                                                                      |
| `src/main/java/com/example/clothingshop/controller/ProductController.java` | Nâng cấp hàm `save()` và `update()` hỗ trợ nhận file ảnh `MultipartFile imageFile` từ máy tính và lưu vào `src/main/resources/static/images/`                                                                            |
| `src/main/resources/templates/admin/product-form.html` | Thêm `enctype="multipart/form-data"`, ô chọn file ảnh từ máy tính (`accept="image/*"`) và khung xem trước ảnh (preview) trước khi lưu                                                                                    |
| `src/main/resources/templates/admin/product-edit.html` | Thêm `enctype="multipart/form-data"`, cho phép tải ảnh mới từ máy tính để thay thế ảnh cũ và xem trước ảnh ngay trên form                                                                                                |

## 3. Chức năng đã hoàn thành (10 việc bổ sung Deadline 2)

| # | Nhiệm vụ | Trạng thái |
|---|---|---|
| 1 | Đồng bộ thanh menu và bố cục Bootstrap 5 giữa các trang cửa hàng & quản trị | Xong |
| 2 | Làm nổi bật (active tab) mục menu tương ứng với trang đang mở bằng Thymeleaf + `GlobalMenuAdvice` | Xong |
| 3 | Hiển thị biểu tượng tài khoản kèm tên và vai trò trên thanh điều hướng | Xong |
| 4 | Phân tách menu theo vai trò; ẩn Giỏ hàng và nút mua hàng đối với `ADMIN` và `STAFF` | Xong |
| 5 | Thêm nút **Mua ngay** trực tiếp trên thẻ sản phẩm ở Trang chủ và Danh sách sản phẩm | Xong |
| 6 | Thêm nút **Thêm vào giỏ** trực tiếp trên thẻ sản phẩm ở Trang chủ và Danh sách sản phẩm | Xong |
| 7 | Bổ sung lối vào **Quản lý đơn hàng** (`/staff/orders`) trên thanh menu và Dashboard của `ADMIN` | Xong |
| 8 | Cho phép Admin chọn và tải ảnh sản phẩm trực tiếp từ máy tính (`MultipartFile`) khi thêm/sửa | Xong |
| 9 | Hiển thị khung xem trước hình ảnh (preview) ngay sau khi chọn tệp trên form thêm/sửa sản phẩm | Xong |
| 10 | Kiểm tra hiển thị Responsive trên màn hình điện thoại và đồng bộ Git theo nhánh riêng | Xong |

## 3b. Dữ liệu test dùng để demo

* **Tài khoản kiểm thử phân quyền (mật khẩu chung `Shop123456@`):**
    * Tài khoản **Admin**: kiểm tra thanh menu `CLOTHING ADMIN`, nút **Đơn hàng** (`/staff/orders`), ẩn Giỏ hàng/nút mua hàng, và tính năng upload ảnh mới tại `/admin/products/add`.
    * Tài khoản **Staff** (`staff01@clothingshop.local`): kiểm tra ẩn Giỏ hàng và nút mua hàng trên thẻ sản phẩm.
    * Tài khoản **Customer** (`customer01@clothingshop.local`): kiểm tra hiển thị **Giỏ hàng**, **Đơn hàng của tôi**, icon tài khoản và nút **Thêm vào giỏ** / **Mua ngay** trên thẻ sản phẩm.

## 4. Cách chạy

```text
# 1. Khởi động SQL Server và đảm bảo database ClothingShopDB đang hoạt động
# 2. Chạy ứng dụng Spring Boot (cổng 8081)
mvnw spring-boot:run          # hoặc chạy ClothingShopApplication trong IntelliJ IDEA

# 3. Các đường dẫn kiểm tra chính trên trình duyệt:
# - Trang chủ & Thẻ sản phẩm:      http://localhost:8081/
# - Danh sách sản phẩm:            http://localhost:8081/products
# - Tổng quan & Menu Admin:        http://localhost:8081/admin
# - Thêm sản phẩm & Upload ảnh:    http://localhost:8081/admin/products/add
