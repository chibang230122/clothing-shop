NOTE: Chưa có nút huỷ đơn hàng ở src/main/resources/templates/account/order-detail.html
1.CartService.java
Lấy danh sách sản phẩm trong giỏ.
Thêm sản phẩm vào giỏ.
Nếu sản phẩm đã tồn tại thì cộng số lượng.
Kiểm tra số lượng không vượt tồn kho.
Cập nhật số lượng.
Xóa một sản phẩm.
Xóa toàn bộ giỏ hàng.
Mỗi người dùng chỉ thao tác trên giỏ hàng của chính mình thông qua email đăng nhập.
2.CartController.java
kết nối CartService với trình duyệt. Xử lý các đường dẫn:
Đường dẫn			Chức năng
GET /cart			Mở trang giỏ hàng
POST /cart/add			Thêm sản phẩm
POST /cart/update		Cập nhật số lượng
POST /cart/remove		Xóa sản phẩm
3.templates/cart/index.html
giao diện giỏ hàng
4.CheckoutForm
chứa thông tin khách hàng nhập khi đặt hàng: tên người nhận, số điện thoại, địa chỉ, phương thức thanh toán và ghi chú.
Các trường đều có kiểm tra dữ liệu đầu vào.
5.OrderService
Nghiệp vụ			Cách xử lý
Kiểm tra giỏ hàng		Không cho đặt hàng khi giỏ trống
Kiểm tra tồn kho		Đọc tồn kho mới nhất trong database
Chống mua vượt tồn		Khóa bản ghi bằng PESSIMISTIC_WRITE
Tính tiền			Lấy BasePrice từ database
Lưu giá mua			Lưu vào OrderItem.UnitPrice
Tạo đơn				Lưu ShopOrder
Tạo chi tiết			Lưu từng OrderItem
Trừ kho				Cập nhật StockQuantity
Xóa giỏ				Chỉ xóa sau khi xử lý đặt hàng
6.CheckoutController.java
kết nối giỏ hàng với chức năng đặt hàng. Khi khách bấm nút thanh toán, Controller sẽ:
- Mở trang nhập thông tin giao hàng.
- Nhận thông tin từ CheckoutForm.
- Gọi OrderService để xử lý tạo đơn.
- Nếu thành công, chuyển sang trang chi tiết đơn hàng.
- Nếu thất bại, thông báo lỗi và giữ khách ở trang checkout.
  7.templates/checkout/index.html
  giao diện thanh toán
  Hiển thị danh sách sản phẩm trong giỏ hàng.
  Hiển thị size, màu sắc, số lượng và giá.
  Tính và hiển thị tổng tiền.
  Cho khách nhập tên người nhận.
  Nhập số điện thoại.
  Nhập địa chỉ giao hàng.
  Chọn phương thức thanh toán.
  Nhập ghi chú.
  Gửi thông tin đến CheckoutController.
  8.OrderCancellationService
  Khi khách hủy đơn đang ở trạng thái PENDING:
- Kiểm tra đơn có đúng của tài khoản đang đăng nhập không.
- Chỉ cho phép hủy đơn PENDING.
- Hoàn lại số lượng hàng vào kho.
- Chuyển trạng thái đơn thành CANCELLED.
- Thực hiện trong một transaction để tránh hủy đơn nhưng không hoàn kho.
  9.OrderCancellationController
  Khi khách gửi yêu cầu POST đến:
  /account/orders/1/cancel
  Controller sẽ:
- Lấy ID đơn hàng.
- Lấy email tài khoản đang đăng nhập.
- Gọi OrderCancellationService.
- Nếu thành công, thông báo hủy đơn.
- Nếu thất bại, hiển thị lỗi.
- Quay lại trang chi tiết đơn hàng.

10.OrderManagementService
chịu trách nhiệm xử lý nghiệp vụ cập nhật trạng thái đơn hàng dành cho Admin/Staff.

11.StaffOrderController
Hiển thị danh sách tất cả đơn hàng.
Nhận yêu cầu cập nhật trạng thái.
Gọi OrderManagementService kiểm tra và xử lý.
Chỉ tài khoản Admin/Staff mới truy cập được theo cấu hình bảo mật hiện tại.

12.templates/staff/orders.html
giao diện quản lý đơn hàng dành cho nhân viên Staff

13.OrderServiceTests
Kiểm tra xử lý nghiệp vụ đơn hàng.
Kiểm tra dữ liệu đơn hàng trong quá trình xử lý.
Kiểm tra các trường hợp hợp lệ và không hợp lệ.
Kiểm tra cách hệ thống phản hồi khi xảy ra lỗi.

14.OrderWorkflowServiceTests
Kiểm tra dữ liệu và điều kiện trước khi đặt hàng.
Kiểm tra xử lý giỏ hàng khi checkout.
Kiểm tra việc tạo đơn hàng và chi tiết đơn hàng.
Kiểm tra xử lý khi quy trình đặt hàng gặp lỗi.

15.OrderTransactionIntegrationTests
Kiểm tra khi khách đặt hàng nhưng số lượng tồn kho không đủ:
- Hệ thống từ chối đặt hàng.
- Không tạo đơn hàng thành công.
- Không để lại dữ liệu Order/OrderItem dở dang.
- Không trừ tồn kho sai.
- Transaction được rollback.

16.OrderCancellationServiceTests
Kiểm tra hủy đơn, hoàn kho, không cho hủy đơn sai trạng thái và không cho hủy đơn của người khác.

17.OrderManagementServiceTests
Kiểm tra cập nhật trạng thái đơn hàng đúng quy trình và từ chối trạng thái không hợp lệ.