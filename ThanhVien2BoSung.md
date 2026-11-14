# Ghi nhận thay đổi dự án Clothing Shop

## Bổ sung chỉnh sửa khách hàng từ trang quản trị

Trong **Quản lý tài khoản**, mỗi dòng CUSTOMER có nút **Chỉnh sửa thông tin**. Admin có thể sửa họ tên, số điện thoại và địa chỉ. Email hiển thị ở chế độ chỉ đọc. GET/POST `/admin/accounts/{id}/edit` trả 403 cho Staff và Customer. Nếu ID không tồn tại hoặc thuộc vai trò khác, Admin nhận 404.

| File | Thay đổi |
| --- | --- |
| `src/main/resources/templates/admin/accounts.html` | Thêm nút chỉnh sửa vào cột Thao tác. |
| `src/main/resources/templates/admin/customer-edit.html` | Thêm biểu mẫu chỉnh sửa thông tin khách hàng. |
| `src/main/java/com/example/clothingshop/controller/AdminAccountController.java` | Thêm URL mở/lưu biểu mẫu và kiểm tra dữ liệu. |
| `src/main/java/com/example/clothingshop/service/AdminAccountService.java` | Tìm và cập nhật hồ sơ theo ID khách hàng. |
| `src/main/java/com/example/clothingshop/service/AccountService.java` | Dùng chung cách cập nhật họ tên, số điện thoại và địa chỉ. |
| `src/test/java/com/example/clothingshop/AccountAccessIntegrationTests.java` | Kiểm tra nút, cập nhật hồ sơ và quyền truy cập của ba vai trò. |

## Tính năng

- Thêm nút **Hiện/Ẩn** riêng cho từng ô mật khẩu ở trang đăng nhập, đăng ký, đổi mật khẩu và đặt lại mật khẩu.
- Thanh điều hướng hiển thị **họ tên · vai trò** từ tài khoản đang đăng nhập. Nhấn vào tên để mở trang **Chỉnh sửa hồ sơ** và cập nhật họ tên, số điện thoại, địa chỉ. Tên mới hiển thị sau khi lưu. Email đăng nhập chỉ đọc.
- Liên kết **Quên mật khẩu?** có sẵn ở trang đăng nhập, kể cả sau khi nhập sai mật khẩu. Mã 6 chữ số được gửi qua email của tài khoản đang hoạt động. OTP hết hạn sau 10 phút, cho tối đa 5 lần thử, dùng một lần và gắn với phiên đã yêu cầu mã. Có thể yêu cầu gửi lại sau ít nhất 60 giây. Sau khi đặt lại thành công, người dùng đăng nhập bằng mật khẩu mới và các phiên đăng nhập cũ bị hết hiệu lực.
- Spring Security chặn Admin và Staff khi mở giỏ hàng, thao tác trên giỏ hoặc gọi checkout trực tiếp. Dịch vụ `OrderService.checkout` kiểm tra lại vai trò và trạng thái tài khoản trước khi đọc giỏ hàng hay ghi đơn. Admin và Staff vẫn dùng trang quản lý đơn để cập nhật trạng thái.

## Ma trận quyền URL

| URL | ADMIN | STAFF | CUSTOMER |
| --- | --- | --- | --- |
| `/`, `/products`, `/products/{id}` | Cho phép | Cho phép | Cho phép |
| `/login`, `/register` | Công khai | Công khai | Công khai |
| `/forgot-password`, `/reset-password` | Chuyển tới đổi mật khẩu khi đã đăng nhập | Tương tự | Tương tự |
| `/account/profile`, `/account/change-password` GET/POST | Hồ sơ của mình | Hồ sơ của mình | Hồ sơ của mình |
| `/cart` GET, `/cart/add`, `/cart/update`, `/cart/remove` POST | 403 | 403 | Cho phép |
| `/checkout` GET/POST | 403 | 403 | Cho phép |
| `/account/orders`, `/account/orders/{id}`, `/account/orders/{id}/cancel` | 403 | 403 | Chỉ đơn của mình |
| `/staff/orders`, `/staff/orders/{id}/status` | Cho phép | Cho phép | 403 |
| `/admin/accounts/{id}/edit` GET/POST | Sửa hồ sơ khách hàng | 403 | 403 |
| `/admin` và các URL quản lý tài khoản, sản phẩm, biến thể, danh mục | Cho phép | 403 | 403 |
| `/logout` POST có CSRF | Cho phép | Cho phép | Cho phép |
| Đường dẫn ngoài phạm vi được cấp quyền | 403 | 403 | 403 |

## Kiểm thử

- Kết quả Maven trên Java 21 và H2 ở chế độ tương thích SQL Server **91 bài đạt, 0 lỗi, 0 bỏ qua**.
- `AccountAccessIntegrationTests` đăng nhập qua biểu mẫu bằng ba tài khoản `admin@example.test`, `staff@example.test`, `customer@example.test` được tạo tự động trong H2. Bài kiểm thử duyệt 50 tổ hợp phương thức và URL từ các controller cho mỗi vai trò. Các URL bị cấm trả 403 trước khi tới controller. Với thao tác cần dữ liệu cụ thể, bài kiểm tra quyền có thể dùng ID thử nghiệm hoặc dữ liệu thiếu để xác nhận yêu cầu tới được controller.
- Kiểm thử riêng xác nhận khách hàng thêm sản phẩm vào giỏ, tạo đơn và chỉ xem được đơn của mình. Gửi cùng nội dung checkout bằng Admin/Staff trả 403. Gọi trực tiếp dịch vụ với Admin/Staff cũng bị từ chối. Phiên khách hàng cũ mất quyền giỏ hàng và checkout khi tài khoản được đổi thành Staff.
- Kiểm thử OTP xác nhận yêu cầu có CSRF, email đúng người nhận, mã sai bị từ chối, mã chỉ hoạt động trong phiên đã yêu cầu, mật khẩu cũ và phiên đăng nhập cũ mất hiệu lực sau khi đặt lại. Dùng bộ gửi mail giả lập trong kiểm thử. Chưa kiểm thử hộp thư SMTP thật hoặc SQL Server thật trong môi trường bàn giao.

Chạy lại các bài kiểm thử phân quyền và tạo đơn trên Windows với JDK 17 trở lên:

```powershell
.\mvnw.cmd "-Dtest=AccountAccessIntegrationTests,SecurityWebTests,OrderServiceTests" test
```

## Gửi OTP qua email không cần mật khẩu ứng dụng Google

Màn hình nhập OTP có nút **Quay lại đăng nhập** dưới liên kết **Nhập email khác**.

### Gửi vào hộp thư thật qua Brevo

Trong Brevo, cấu hình địa chỉ gửi theo hướng dẫn của dịch vụ và lấy **SMTP login** cùng **SMTP key**. Mở PowerShell tại thư mục chứa `mvnw.cmd`, giữ cấu hình SQL Server đang sử dụng, rồi chạy:

```powershell
$env:BREVO_SMTP_LOGIN = "SMTP_LOGIN_CUA_BAN"
$env:BREVO_FROM = "DIA_CHI_GUI_DA_CAU_HINH"
$env:BREVO_SMTP_KEY = Read-Host "Nhap SMTP key cua Brevo" -MaskInput
.\mvnw.cmd "-Dspring-boot.run.profiles=otp-brevo" spring-boot:run
```

Ở trang **Quên mật khẩu?**, chỉ cần nhập email thật đã đăng ký tài khoản Clothing Shop. SMTP key do người vận hành cấu hình phía máy chủ. Địa chỉ mẫu `@clothingshop.local` dùng được trong hộp thư thử nghiệm bên dưới.

[Tài liệu SMTP Brevo](https://help.brevo.com/hc/en-us/articles/7924908994450-Send-transactional-emails-using-Brevo-SMTP)

### Demo trên máy không cần tài khoản hay mật khẩu SMTP

Tải Mailpit cho Windows, giải nén và chạy `mailpit.exe`. Trong terminal của dự án, chạy:

```powershell
.\mvnw.cmd "-Dspring-boot.run.profiles=otp-local" spring-boot:run
```

Nhập email của một tài khoản Clothing Shop đang hoạt động, kể cả `admin@clothingshop.local`, rồi mở [hộp thư Mailpit](http://localhost:8025) để lấy OTP. Email demo được thu vào Mailpit trên máy.

[Tài liệu cài Mailpit](https://mailpit.axllent.org/docs/install/)

### File thêm hoặc sửa cho cấu hình OTP

| File | Thay đổi |
| --- | --- |
| `src/main/resources/application-otp-brevo.properties` | Cấu hình SMTP Brevo để gửi vào hộp thư thật. |
| `src/main/resources/application-otp-local.properties` | Cấu hình gửi vào Mailpit qua localhost. |
| `src/main/java/com/example/clothingshop/service/PasswordResetService.java` | Kiểm tra thông tin SMTP khi máy chủ yêu cầu xác thực. |
| `src/test/java/com/example/clothingshop/AccountAccessIntegrationTests.java` | Khai báo SMTP giả lập không yêu cầu xác thực. |

## Đối chiếu bản clothing-shop-main(1).zip

So với bản ZIP gốc, bản bạn gửi có **14 tệp dự án được thêm, 24 tệp được sửa và 0 tệp bị xóa**. Số tệp thêm gồm tài liệu này và lớp kiểm thử trống được ghi bên dưới. Đối chiếu theo nội dung từng tệp, tính cả thay đổi khoảng trắng.

- `src/main/java/com/example/clothingshop/ClothingShopApplication.java` có thêm một dòng trống ở cuối tệp.
- `src/test/java/AccountAccessIntegrationTests.java` là lớp trống được thêm ở thư mục gốc của mã kiểm thử. Bộ kiểm thử tích hợp nằm tại `src/test/java/com/example/clothingshop/AccountAccessIntegrationTests.java`.
- `src/main/resources/application.properties` còn có cấu hình tài khoản kết nối SQL Server đã được điền trong bản bạn gửi. Tài liệu này không ghi lại giá trị đăng nhập.
- ZIP còn chứa **164 tệp trong `target/`**, **6 tệp trong `.idea/`** và **1 tệp nhật ký `logs/clothing-shop.log`**. Ba nhóm này được ghi riêng và nằm ngoài số tệp dự án ở trên.

Kết quả 91 bài kiểm thử ở phần trước thuộc lần chạy trên H2 của bản bàn giao. Lần đối chiếu ZIP này kiểm tra danh sách và nội dung tệp.

## Danh sách file

### File thêm

| File | Nội dung |
| --- | --- |
| `src/main/java/com/example/clothingshop/config/PasswordResetConfig.java` | Tạo bộ thực thi nền có giới hạn cho tác vụ email. |
| `src/main/java/com/example/clothingshop/controller/AccountNavigationAdvice.java` | Cung cấp họ tên và vai trò cho thanh điều hướng. |
| `src/main/java/com/example/clothingshop/controller/PasswordResetController.java` | Nhận yêu cầu OTP và đổi mật khẩu. |
| `src/main/java/com/example/clothingshop/service/PasswordResetService.java` | Tạo, gửi và xác minh OTP, cập nhật mật khẩu và hết hiệu lực phiên cũ. |
| `src/main/resources/static/js/password-visibility.js` | Chuyển trạng thái hiển thị của từng ô mật khẩu. |
| `src/main/resources/templates/auth/forgot-password.html` | Form nhập email nhận OTP. |
| `src/main/resources/templates/auth/reset-password.html` | Form nhập OTP, mật khẩu mới và xác nhận. |
| `src/main/resources/templates/fragments/account-controls.html` | Nút Hiện/Ẩn và liên kết tên, vai trò dùng chung. |
| `src/test/java/com/example/clothingshop/AccountAccessIntegrationTests.java` | Kiểm thử ma trận URL, hồ sơ, đặt hàng và đặt lại mật khẩu. |
| `GHI_NHAN_THAY_DOI.md` | Danh sách thay đổi và hướng dẫn kiểm thử, cấu hình. |

### File sửa

| File | Nội dung |
| --- | --- |
| `pom.xml` | Thêm Spring Boot Mail và H2 cho kiểm thử. |
| `src/main/java/com/example/clothingshop/config/SecurityConfig.java` | Phân quyền URL, kiểm tra quyền ghi nhận trong cơ sở dữ liệu ở luồng mua hàng và quản lý phiên đăng nhập. |
| `src/main/java/com/example/clothingshop/controller/CheckoutController.java` | Trả 403 khi dịch vụ từ chối quyền đặt hàng. |
| `src/main/java/com/example/clothingshop/entity/User.java` | Chỉ ghi các cột thực sự thay đổi khi lưu tài khoản, tránh ghi đè mật khẩu khi sửa hồ sơ đồng thời. |
| `src/main/java/com/example/clothingshop/repository/UserRepository.java` | Truy vấn vai trò khách hàng đang hoạt động và cập nhật mật khẩu có điều kiện. |
| `src/main/java/com/example/clothingshop/service/CustomUserDetailsService.java` | Kiểm tra trạng thái và vai trò khách hàng hiện tại. |
| `src/main/java/com/example/clothingshop/service/OrderService.java` | Chặn tài khoản ngoài vai trò CUSTOMER hoặc đã vô hiệu hóa ở backend. |
| `src/main/resources/application.properties` | Đọc cấu hình SMTP từ biến môi trường. |
| `src/main/resources/templates/account/change-password.html` | Thêm nút Hiện/Ẩn cho các ô mật khẩu. |
| `src/main/resources/templates/account/profile.html` | Làm rõ trang chỉnh sửa và bổ sung gợi ý nhập dữ liệu. |
| `src/main/resources/templates/auth/login.html` | Thêm nút Hiện/Ẩn, liên kết quên mật khẩu và thông báo kết quả. |
| `src/main/resources/templates/auth/register.html` | Thêm nút Hiện/Ẩn. |
| `src/main/resources/templates/fragments/admin-menu.html` | Hiển thị họ tên và vai trò trên thanh quản trị. |
| `src/main/resources/templates/fragments/bootstrap-menu.html` | Hiển thị họ tên, vai trò và liên kết tới hồ sơ. |
| `src/main/resources/templates/fragments/header.html` | Nạp JavaScript cho nút Hiện/Ẩn. |
| `src/main/resources/templates/fragments/menu.html` | Hiển thị danh tính và liên kết giỏ hàng/quản lý đơn theo vai trò ở trang chủ. |
| `src/main/resources/templates/product-detail.html` | Hiện thao tác thêm giỏ cho khách hàng và liên kết đăng nhập cho khách vãng lai. |
| `src/test/java/com/example/clothingshop/OrderServiceTests.java` | Kiểm tra chặn gọi trực tiếp nghiệp vụ checkout bằng Admin và Staff. |
| `src/test/java/com/example/clothingshop/SecurityWebTests.java` | Bổ sung repository giả lập cần cho kiểm thử MVC. |

### File đã xóa

Không có. Đối chiếu danh sách tệp trong bản ZIP gốc với bản ZIP cập nhật cho thấy tất cả tệp gốc vẫn có mặt.
