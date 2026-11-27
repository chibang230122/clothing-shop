Mình đã bổ sung chức năng khởi tạo mật khẩu cho các tài khoản STAFF và CUSTOMER để mọi người có thể đăng nhập kiểm thử.

Thông tin tài khoản sau khi khởi tạo và tất cả đều có chung mk là `Shop123456@`:

- STAFF: `staff01@clothingshop.local`
- CUSTOMER: `customer01@clothingshop.local` đến `customer20@clothingshop.local`
- Mật khẩu kiểm thử: `Shop123456@`

Cách thực hiện:
1. Pull code mới nhất từ GitHub.
2. Mở `application.properties`.
3. Đổi `app.demo-password-reset.enabled=false` thành `true`.
4. Chạy project Spring Boot một lần.
5. Kiểm tra database xem PasswordHash đã được mã hóa BCrypt chưa.
6. Đổi lại thành `false` và chạy lại project.

Lưu ý: Chức năng này chỉ cập nhật mật khẩu của các tài khoản đang có giá trị `REPLACE_WITH_BCRYPT_HASH`, không ghi đè mật khẩu đã tồn tại.