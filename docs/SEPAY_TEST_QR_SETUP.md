# Tích hợp QR demo và SePay Test Mode

## 1. Chế độ mặc định để bảo vệ đồ án (không cần SePay)

Code mặc định chạy `PAYMENT_MODE=DEMO`:

- Khách chọn **Chuyển khoản ngân hàng** ở checkout.
- Đơn được tạo với `PaymentStatus = UNPAID`; trang chi tiết đơn hiển thị QR nếu đã cấu hình tài khoản QR.
- Nút **Mô phỏng: Tôi đã quét QR** chỉ mô phỏng thao tác trong đồ án và đổi trạng thái sang `PAID`.
- Không có giao dịch ngân hàng và không có tiền thật. Không được dùng chế độ này cho website bán hàng thật.
- COD không hiển thị QR và không được đánh dấu đã thanh toán lúc tạo đơn.

Camera/app ngân hàng chỉ đọc nội dung QR; nó không gửi sự kiện “đã quét” về website. Vì vậy demo phải có nút mô phỏng, hoặc dùng SePay Test Mode để gửi giao dịch giả lập qua webhook.

## 2. Tạo tài khoản và QR trong SePay Test Mode

1. Đăng nhập https://my.sepay.vn/ và mở **Test mode** ở thanh bên. Nếu không thấy, tài khoản cần được cấp quyền hoặc bật Test Mode.
2. Vào **Test mode → Tài khoản ngân hàng → Thêm tài khoản** và tạo tài khoản thử nghiệm.
3. Ghi lại mã ngân hàng (bank code) và số tài khoản thử nghiệm. Đây là dữ liệu test, không phải tài khoản nhận tiền thật.
4. Cấu hình mã thanh toán để SePay nhận diện nội dung đơn hàng. Mã đơn của ứng dụng bắt đầu bằng `DH` và có 20 ký tự hex sau đó. Cấu hình tiền tố/mẫu nhận diện phù hợp với mã `DH...`.
5. Đặt biến môi trường cho tiến trình chạy ứng dụng:

   - `PAYMENT_MODE=SEPAY_TEST`
   - `PAYMENT_BANK_CODE=<bank-code-test>`
   - `PAYMENT_ACCOUNT_NUMBER=<test-account-number>`
   - `PAYMENT_ACCOUNT_NAME=<test-account-name>` (tuỳ chọn)
   - `SEPAY_WEBHOOK_API_KEY=<api-key-tu-sepay>`

QR được dựng theo hướng dẫn VietQR của SePay (`vietqr.app/img`) và chỉ điền sẵn tài khoản, số tiền, nội dung. Việc quét QR không tự làm đơn thành công.

## 3. Tạo webhook SePay Test Mode

1. Mở **Test mode → Tích hợp Webhook → Thêm webhook**.
2. Tên: `Clothing Shop Test`.
3. Cách nhận: **Gửi tới URL của tôi**.
4. URL: `https://<dia-chi-cong-khai>/webhook/sepay`.
5. Loại giao dịch: **Tiền vào**; định dạng: **JSON**.
6. Chọn tài khoản ngân hàng Test Mode vừa tạo.
7. Bật **Dùng để xác thực thanh toán** nếu giao diện có tuỳ chọn này.
8. Bảo mật: chọn **API Key**, đặt cùng giá trị với `SEPAY_WEBHOOK_API_KEY`.
9. Lưu webhook.

SePay phải gọi được URL từ Internet. Khi chạy ứng dụng local ở cổng `8081`, có thể dùng ngrok:

```powershell
ngrok http 8081
```

Lấy HTTPS URL ngrok rồi ghép `/webhook/sepay`, ví dụ `https://xxxx.ngrok-free.app/webhook/sepay`, nhập URL đó trong SePay. Không dùng `localhost` làm URL webhook.

## 4. Chạy ứng dụng trên Windows PowerShell

Đóng tiến trình Java đang chạy, mở PowerShell tại thư mục có `pom.xml`, sau đó chạy:

```powershell
$env:PAYMENT_MODE = "SEPAY_TEST"
$env:PAYMENT_BANK_CODE = "<bank-code-test>"
$env:PAYMENT_ACCOUNT_NUMBER = "<test-account-number>"
$env:PAYMENT_ACCOUNT_NAME = "CLOTHING SHOP TEST"
$env:SEPAY_WEBHOOK_API_KEY = "<api-key-tu-sepay>"
.\mvnw.cmd spring-boot:run
```

Nếu chỉ demo nút mô phỏng, không cần các biến trên; để `PAYMENT_MODE=DEMO` hoặc bỏ biến này.

## 5. Kiểm thử

1. Đăng nhập tài khoản CUSTOMER.
2. Thêm sản phẩm vào giỏ → checkout.
3. Chọn **Chuyển khoản ngân hàng** → đặt hàng.
4. Mở trang chi tiết đơn, kiểm tra QR, số tiền và mã đơn `DH...`.
5. Với `DEMO`: bấm **Mô phỏng: Tôi đã quét QR**. Website cập nhật `PAID`.
6. Với `SEPAY_TEST`: vào Dashboard SePay Test Mode → **Mô phỏng giao dịch**, tạo giao dịch tiền vào với số tiền bằng hoặc lớn hơn tổng đơn và nội dung chứa đúng mã đơn `DH...`. SePay gửi webhook; trang đơn sẽ tự kiểm tra trạng thái mỗi 3 giây.
7. Đơn COD không hiển thị QR; không dùng nút demo; đơn ví điện tử đã bỏ khỏi lựa chọn vì project chưa tích hợp cổng ví.

## Lưu ý an toàn

- Test Mode của SePay tách biệt với Live và giao dịch mô phỏng không chạm tiền thật.
- Không commit API Key/secret lên GitHub. Dùng biến môi trường.
- Không dùng nút DEMO hoặc tài khoản Test Mode để nhận thanh toán thật.
- Webhook chỉ chấp nhận `Authorization: Apikey <key>`, giao dịch tiền vào, đúng mã đơn và số tiền đủ. Webhook gửi lặp không làm đơn được cộng/đánh dấu nhiều lần.

## 6. Muốn thử thao tác quét QR giả lập ngay trong SePay

Trong Dashboard đang bật Test mode, mở **Mô phỏng tạo mã VietQR**:

1. Chọn tài khoản ngân hàng Test Mode.
2. Nhập số tiền của đơn hàng.
3. Nhập đúng mã đơn `DH...` vào nội dung chuyển khoản.
4. Xem QR và bấm **Mô phỏng chuyển khoản**.
5. Trong màn điện thoại giả lập, bấm xác nhận quét rồi bấm Chuyển khoản.
6. SePay tạo giao dịch sandbox và kích hoạt webhook. Trang đơn Clothing Shop sẽ tự chuyển sang `PAID` nếu webhook khớp mã đơn và số tiền.

Lưu ý: QR Test Mode dùng tài khoản giả lập nên ứng dụng ngân hàng thật sẽ báo tài khoản không tồn tại. Không thể dùng app ngân hàng thật để chuyển tiền hoặc xác nhận. Hãy dùng đúng màn mô phỏng VietQR của SePay; giao dịch sandbox không tác động tiền thật.
