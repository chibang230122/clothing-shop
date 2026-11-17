# GHI CHÚ BỔ SUNG THÀNH VIÊN 4 – DEADLINE 2

Phạm vi ghi chú: các phần được bổ sung/chỉnh sửa trong source code cho Deadline 2.

## 1. Chức năng Mua ngay

| File | Nội dung đã bổ sung |
|---|---|
| `src/main/java/com/example/clothingshop/service/OrderService.java` | Thêm hàm `checkoutNow(...)` để xử lý đặt mua trực tiếp một biến thể sản phẩm; kiểm tra người mua, thông tin giao hàng, sản phẩm và tồn kho; khóa biến thể khi xử lý; lấy giá từ database; tạo đơn hàng và chi tiết đơn; trừ tồn kho trong transaction. |
| `src/main/java/com/example/clothingshop/controller/CheckoutController.java` | Thêm hai endpoint GET/POST `/checkout/buy-now/{variantId}` để mở trang mua ngay và tiếp nhận thông tin đặt hàng. |
| `src/main/resources/templates/product-detail.html` | Thêm nút “Mua ngay” bên cạnh chức năng “Thêm vào giỏ hàng” cho biến thể sản phẩm còn hàng. |
| `src/main/resources/templates/checkout/buy-now.html` | Tạo giao diện mua ngay, hiển thị sản phẩm và cho khách nhập thông tin người nhận, số điện thoại, địa chỉ, phương thức thanh toán và ghi chú. |

## 2. Quản lý đơn hàng Staff

| File | Nội dung đã bổ sung |
|---|---|
| `src/main/java/com/example/clothingshop/controller/StaffOrderController.java` | Bổ sung tìm kiếm đơn hàng theo mã đơn, tên khách hàng, email và số điện thoại; lọc theo trạng thái và khoảng ngày; phân trang 10 đơn mỗi trang; sắp xếp đơn mới nhất trước; bổ sung đường dẫn xem chi tiết đơn hàng. |
| `src/main/java/com/example/clothingshop/repository/ShopOrderRepository.java` | Thêm truy vấn `searchStaffOrders(...)` phục vụ tìm kiếm, lọc và phân trang; thêm `countByOrderStatus(...)` để đếm đơn theo trạng thái. |
| `src/main/resources/templates/staff/orders.html` | Bổ sung ô tìm kiếm, bộ lọc trạng thái, bộ lọc ngày, nút xem chi tiết, điều hướng phân trang và khu vực thao tác cập nhật trạng thái đơn hàng. |
| `src/main/resources/templates/staff/order-detail.html` | Tạo giao diện chi tiết đơn hàng dành cho Staff, hiển thị thông tin khách hàng/người nhận, địa chỉ, sản phẩm, phân loại, số lượng, đơn giá và tổng tiền. |

## 3. Dashboard Staff

| File | Nội dung đã bổ sung |
|---|---|
| `src/main/java/com/example/clothingshop/controller/StaffDashboardController.java` | Tạo controller cho `/staff/dashboard`; lấy tổng số đơn theo các trạng thái PENDING, CONFIRMED, PROCESSING, SHIPPING, COMPLETED và CANCELLED. |
| `src/main/resources/templates/staff/dashboard.html` | Tạo giao diện Dashboard Staff, hiển thị số lượng đơn theo từng trạng thái và liên kết đến trang quản lý đơn hàng. |
| `src/main/resources/templates/fragments/bootstrap-menu.html` | Thêm liên kết Dashboard Staff cho tài khoản ADMIN/STAFF trong menu điều hướng. |
| `src/main/resources/templates/fragments/admin-menu.html` | Thêm liên kết Dashboard Staff trong menu quản trị. |

## 4. Thông báo khi khách hủy đơn

| File | Nội dung đã bổ sung |
|---|---|
| `src/main/java/com/example/clothingshop/controller/OrderCancellationController.java` | Bổ sung thông báo lỗi dự phòng khi xử lý yêu cầu hủy đơn phát sinh ngoại lệ không có nội dung thông báo. |
| `src/main/resources/templates/account/order-detail.html` | Bổ sung khu vực hiển thị thông báo hủy đơn thành công hoặc thông báo lỗi khi khách thao tác hủy đơn. |

## 5. Tổng hợp file

### File tạo mới

- `src/main/java/com/example/clothingshop/controller/StaffDashboardController.java`
- `src/main/resources/templates/staff/dashboard.html`
- `src/main/resources/templates/staff/order-detail.html`
- `src/main/resources/templates/checkout/buy-now.html`

### File chỉnh sửa

- `src/main/java/com/example/clothingshop/service/OrderService.java`
- `src/main/java/com/example/clothingshop/controller/CheckoutController.java`
- `src/main/java/com/example/clothingshop/controller/StaffOrderController.java`
- `src/main/java/com/example/clothingshop/repository/ShopOrderRepository.java`
- `src/main/java/com/example/clothingshop/controller/OrderCancellationController.java`
- `src/main/resources/templates/product-detail.html`
- `src/main/resources/templates/staff/orders.html`
- `src/main/resources/templates/fragments/bootstrap-menu.html`
- `src/main/resources/templates/fragments/admin-menu.html`
- `src/main/resources/templates/account/order-detail.html`
