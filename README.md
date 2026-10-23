
# Clothing Shop - Quản lý shop quần áo

## 1. Giới thiệu

Dự án xây dựng website quản lý shop quần áo bằng Java Spring Boot,
Thymeleaf và SQL Server.

## 2. Công nghệ sử dụng

- Java 17
- Spring Boot 3.5.6
- Spring Data JPA
- Thymeleaf
- Microsoft SQL Server
- Maven
- Git và GitHub

## 3. Cấu trúc database

Database: `ClothingShopDB`

Các bảng chính:

- Roles
- Users
- Categories
- Products
- ProductVariants
- Orders
- OrderItems
- Carts
- CartItems

## 4. Cài đặt database

1. Mở SQL Server Management Studio (SSMS).
2. Kết nối SQL Server.
3. Mở file script SQL của dự án.
4. Chọn database `ClothingShopDB` hoặc tạo database theo script.
5. Chạy script tạo bảng, khóa ngoại, ràng buộc và dữ liệu mẫu.
6. Kiểm tra các bảng đã được tạo thành công.

## 5. Cấu hình kết nối

Mở file:

`src/main/resources/application.properties`

Cập nhật thông tin kết nối SQL Server theo môi trường máy của bạn.

Không đưa mật khẩu thật hoặc thông tin đăng nhập nhạy cảm lên GitHub.

## 6. Chạy ứng dụng

1. Cài JDK 17.
2. Mở dự án bằng IntelliJ IDEA.
3. Chờ Maven tải các dependencies.
4. Kiểm tra cấu hình SQL Server.
5. Chạy lớp `ClothingShopApplication`.
6. Mở trình duyệt tại:

http://localhost:8081

## 7. Chức năng và truy vấn

- Hiển thị danh sách sản phẩm đang hoạt động.
- Tìm kiếm sản phẩm theo từ khóa, danh mục và khoảng giá.
- Thống kê doanh thu theo khoảng thời gian.

## 8. Lưu ý

- Database phải được khởi động trước khi chạy ứng dụng.
- Tên bảng và cột trong Entity phải khớp với database.
- Dữ liệu mẫu cần được kiểm tra theo yêu cầu đồ án.
