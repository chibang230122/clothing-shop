# Ghi nhận thay đổi Clothing Shop

## Cập nhật quản lý nhân viên

Trong **Admin → Quản lý khách hàng / tài khoản**, chọn **Tạo tài khoản nhân viên**, điền họ tên, email, mật khẩu và xác nhận mật khẩu. Số điện thoại và địa chỉ là hai trường tùy chọn. Tài khoản tạo thành công có vai trò STAFF và đăng nhập được ngay. Để sửa họ tên, số điện thoại hoặc địa chỉ, bấm **Chỉnh sửa thông tin** tại dòng của nhân viên.

Các tệp của riêng lần cập nhật này gồm:

- Thêm `src/main/resources/templates/admin/staff-create.html`.
- Sửa `src/main/resources/templates/admin/accounts.html` và `src/main/resources/templates/admin/customer-edit.html`.
- Sửa `src/main/java/com/example/clothingshop/controller/AdminAccountController.java`.
- Sửa `src/main/java/com/example/clothingshop/service/AdminAccountService.java` và `src/main/java/com/example/clothingshop/service/AuthService.java`.
- Sửa `src/test/java/com/example/clothingshop/AccountAccessIntegrationTests.java` và bản ghi nhận này.

## Hóa đơn bán hàng

Đăng nhập bằng tài khoản STAFF hoặc ADMIN, mở **Đơn hàng → Chi tiết đơn hàng → Xuất hóa đơn**. Nút **Tải hóa đơn HTML** tải tài liệu tiếng Việt có thể mở bằng trình duyệt và in hoặc lưu PDF. Nút **Gửi hóa đơn qua email** lấy địa chỉ từ tài khoản khách đặt đơn và đính kèm cùng nội dung hóa đơn. Số lượng, đơn giá, thành tiền, phí vận chuyển và tổng cộng lấy từ đơn hàng đã lưu.

| Phương thức | API | Kết quả |
|---|---|---|
| GET | `/api/invoices/{id}` | Tệp HTML, UTF-8 |
| POST | `/api/invoices/{id}/email` | JSON thông báo kết quả. Form gửi kèm token CSRF của phiên đăng nhập. |

HTTP 404 khi đơn hàng không tồn tại. API gửi thư trả 200 khi SMTP tiếp nhận, 503 khi thiếu máy chủ hoặc địa chỉ gửi, 502 khi tạo hoặc gửi thư thất bại. Mỗi lần nhấn gửi thành công tương ứng với một email.

## Cấu hình Google và email trên Windows

1. **Cơ sở dữ liệu đang dùng** — Chạy `database/upgrade_google_login.sql` trong SQL Server Management Studio trước khi khởi động phiên bản này, kể cả khi chỉ dùng đăng nhập mật khẩu. Script bổ sung cột và chỉ mục cho tài khoản Google. Với cơ sở dữ liệu mới, dùng `database/ClothingShopDB.sql`. Script khởi tạo này xóa và tạo lại cơ sở dữ liệu cùng tên.
2. **Google** — Trong Google Cloud Console, cấu hình OAuth consent screen và tạo OAuth client loại **Web application**. Thêm chính xác `http://localhost:8081/login/oauth2/code/google` vào **Authorized redirect URIs**. Khi chạy ở tên miền khác, dùng địa chỉ HTTPS của ứng dụng với cùng đường dẫn callback. Nếu ứng dụng ở chế độ Testing, thêm tài khoản thử vào danh sách Test users.
3. **Brevo** — Chuẩn bị SMTP login, SMTP key và địa chỉ gửi đã xác minh. Cấu hình mẫu dưới đây bật cả hai profile. Thay các giá trị trong dấu ngoặc nhọn bằng thông tin riêng, chạy từ thư mục chứa `pom.xml` trong cùng cửa sổ PowerShell.

```powershell
$env:SPRING_PROFILES_ACTIVE = "otp-brevo,google"
$env:GOOGLE_CLIENT_ID = "<OAuth Client ID>"
$env:GOOGLE_CLIENT_SECRET = "<OAuth Client Secret>"
$env:BREVO_SMTP_LOGIN = "<SMTP login>"
$env:BREVO_SMTP_KEY = "<SMTP key>"
$env:BREVO_FROM = "<Email người gửi đã xác minh>"
.\mvnw.cmd spring-boot:run
```

Truy cập `http://localhost:8081/login` và chọn **Đăng nhập bằng Google**. Lần đầu, một email Google đã xác minh và chưa có trong cửa hàng sẽ tạo tài khoản CUSTOMER. Lần sau, hệ thống nhận diện bằng mã người dùng Google. Tài khoản bị khóa hoặc email trùng tài khoản khác sẽ được báo lỗi. Người dùng đã đăng ký bằng mật khẩu tiếp tục dùng nút **Đăng nhập**. Cả hai trang đều hiện nút Google. Khi chưa cấu hình OAuth, nhấn nút sẽ hiện thông báo. Khi đã cấu hình, hai nút mở cùng luồng xác thực của Google.

## Ràng buộc mật khẩu

Đăng ký, đổi mật khẩu và đặt lại bằng OTP đều kiểm tra mật khẩu ở phía máy chủ. Mật khẩu mới dài 8–50 ký tự, có chữ hoa, chữ thường, chữ số, ít nhất một dấu câu hoặc ký hiệu và dài tối đa 72 byte UTF-8. Ví dụ đạt yêu cầu về định dạng là `AoKhoac@2026`. Khi mới mở form, giao diện ẩn hướng dẫn. Khi nhập thiếu hoặc sai mật khẩu mới, form báo “Mật khẩu cần 8–50 ký tự, có chữ hoa, chữ thường, chữ số và ký hiệu đặc biệt”. Các form vẫn yêu cầu nhập lại mật khẩu. Quy tắc mới áp dụng khi người dùng tạo mật khẩu, nên tài khoản hiện có vẫn đăng nhập được bằng mật khẩu đang dùng.

## Kiểm thử

Đã chạy 108 ca kiểm thử Maven trên Java 17 với H2 và SQL dialect tương thích SQL Server. Toàn bộ đều đạt. Kiểm thử email dùng JavaMailSender thử nghiệm để kiểm tra địa chỉ người nhận và nội dung tệp đính kèm. Kiểm thử Google dùng cấu hình client thử và thông tin định danh mẫu. Chưa đăng nhập bằng tài khoản Google thật hoặc xác nhận thư đến hộp thư thật.

## Tệp đã thêm (13)

| Đường dẫn tương đối từ thư mục dự án | Nội dung |
|---|---|
| `GHI_NHAN_THAY_DOI.md` | Danh sách thay đổi, hướng dẫn cấu hình và kết quả kiểm thử. |
| `database/upgrade_google_login.sql` | Nâng cấp bảng Users cho cơ sở dữ liệu hiện có. |
| `src/main/java/com/example/clothingshop/controller/InvoiceController.java` | API tải hóa đơn và gửi email có tệp đính kèm. |
| `src/main/java/com/example/clothingshop/service/GoogleLoginService.java` | Đối chiếu định danh đã xác minh, khởi tạo khách hàng và ánh xạ quyền. |
| `src/main/java/com/example/clothingshop/service/InvoiceService.java` | Đọc đơn hàng và kết xuất cùng tài liệu cho tải xuống và gửi thư. |
| `src/main/java/com/example/clothingshop/service/PasswordPolicy.java` | Quy tắc dùng chung cho mật khẩu mới. |
| `src/main/resources/application-google.properties` | Cấu hình OAuth client bằng biến môi trường. |
| `src/main/resources/static/images/google-g.png` | Biểu tượng Google hiển thị trên hai nút. |
| `src/main/resources/static/js/invoice-email.js` | Gửi form kèm CSRF, khóa nút trong khi chờ và thông báo kết quả. |
| `src/main/resources/templates/admin/staff-create.html` | Form tạo tài khoản nhân viên. |
| `src/main/resources/templates/invoice/document.html` | Mẫu hóa đơn HTML có nội dung sản phẩm và tổng tiền. |
| `src/test/java/com/example/clothingshop/service/GoogleLoginTests.java` | Kiểm thử chuyển hướng Google, tài khoản trùng và bị khóa. |
| `src/test/java/com/example/clothingshop/service/PasswordPolicyTests.java` | Kiểm thử thành phần, độ dài và biên UTF-8. |

## Tệp đã sửa (26)

| Đường dẫn tương đối từ thư mục dự án | Nội dung |
|---|---|
| `database/ClothingShopDB.sql` | Thêm trường định danh Google và chỉ mục duy nhất cho cơ sở dữ liệu mới. |
| `pom.xml` | Bổ sung Spring Security OAuth2 Client. |
| `src/main/java/com/example/clothingshop/config/SecurityConfig.java` | Cấu hình đăng nhập Google và giới hạn quyền sử dụng API hóa đơn. |
| `src/main/java/com/example/clothingshop/controller/AccountController.java` | Trả lỗi ở đúng ô mật khẩu mới khi đổi mật khẩu. |
| `src/main/java/com/example/clothingshop/controller/AccountNavigationAdvice.java` | Chuyển thông báo ràng buộc mật khẩu từ máy chủ tới form. |
| `src/main/java/com/example/clothingshop/controller/AdminAccountController.java` | Thêm form GET/POST tạo STAFF và cho phép quản trị viên sửa hồ sơ nhân viên. |
| `src/main/java/com/example/clothingshop/controller/AuthController.java` | Kiểm tra mật khẩu và cung cấp trạng thái cấu hình Google cho cả hai trang. |
| `src/main/java/com/example/clothingshop/controller/PasswordResetController.java` | Kiểm tra mật khẩu mới trước khi xác minh OTP. |
| `src/main/java/com/example/clothingshop/dto/ChangePasswordForm.java` | Thống nhất thông báo lỗi cho mật khẩu mới. |
| `src/main/java/com/example/clothingshop/dto/RegisterForm.java` | Dùng chung thông báo PasswordPolicy cho mật khẩu thiếu, sai độ dài hoặc sai thành phần. |
| `src/main/java/com/example/clothingshop/entity/User.java` | Ánh xạ GoogleSubject. |
| `src/main/java/com/example/clothingshop/repository/UserRepository.java` | Tìm tài khoản theo định danh Google. |
| `src/main/java/com/example/clothingshop/service/AccountService.java` | Áp dụng ràng buộc trước khi lưu mật khẩu thay đổi. |
| `src/main/java/com/example/clothingshop/service/AdminAccountService.java` | Cho phép cập nhật thông tin cá nhân của STAFF và CUSTOMER. |
| `src/main/java/com/example/clothingshop/service/AuthService.java` | Dùng chung bước tạo tài khoản, mã hóa mật khẩu và kiểm tra email. Máy chủ gán vai trò theo chức năng. |
| `src/main/java/com/example/clothingshop/service/PasswordResetService.java` | Kiểm tra mật khẩu mới và vô hiệu hóa các phiên liên quan sau khi đặt lại. |
| `src/main/resources/static/js/password-visibility.js` | Hiển thị lỗi ràng buộc của mật khẩu mới khi form không hợp lệ. |
| `src/main/resources/templates/account/change-password.html` | Ẩn hướng dẫn mặc định và chỉ báo lỗi khi mật khẩu mới chưa hợp lệ. |
| `src/main/resources/templates/admin/accounts.html` | Hiển thị nút tạo tài khoản nhân viên và nút sửa hồ sơ. |
| `src/main/resources/templates/admin/customer-edit.html` | Dùng chung form chỉnh sửa hồ sơ cho khách hàng và nhân viên. |
| `src/main/resources/templates/auth/login.html` | Hiển thị nút đăng nhập bằng Google ở đầu form. |
| `src/main/resources/templates/auth/register.html` | Bổ sung nút đăng ký bằng Google và chỉ báo lỗi khi mật khẩu chưa hợp lệ. |
| `src/main/resources/templates/auth/reset-password.html` | Chỉ hiển thị thông báo mật khẩu khi có lỗi. |
| `src/main/resources/templates/fragments/account-controls.html` | Thành phần dùng chung cho nút Google trên trang đăng nhập và đăng ký. |
| `src/main/resources/templates/staff/order-detail.html` | Form tải hóa đơn và gửi email trong trang quản lý đơn. |
| `src/test/java/com/example/clothingshop/AccountAccessIntegrationTests.java` | Kiểm thử phân quyền, hóa đơn, mật khẩu, tạo và sửa STAFF, đăng nhập bằng tài khoản nhân viên vừa tạo. |
