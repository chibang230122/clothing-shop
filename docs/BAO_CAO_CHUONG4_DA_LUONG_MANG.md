# Bản nháp báo cáo Chương 4: Kiểm thử đa luồng và mạng (Thành viên 5)

Gợi ý dùng làm mục 4.x trong Chương 4. Các ô **[ĐIỀN]** phải được điền bằng kết quả thực tế khi chạy trên máy của nhóm (kèm ảnh chụp), không điền theo dự đoán.

## 4.x.1 Mục tiêu và môi trường

Mục tiêu: chứng minh (1) hệ thống không bán vượt tồn kho khi nhiều khách mua cùng lúc và (2) Socket Server phục vụ được nhiều client đồng thời, vẫn hoạt động khi client gửi dữ liệu sai hoặc ngắt đột ngột.

| Thành phần | Giá trị |
|---|---|
| JDK | 17 **[ĐIỀN phiên bản chính xác]** |
| Spring Boot | 3.5.6 |
| SQL Server | **[ĐIỀN phiên bản]**, database `ClothingShopDB` |
| Công cụ chạy test | `mvnw test` (JUnit 5) |
| Số client demo | 3 tiến trình Java riêng |

## 4.x.2 Kiểm thử đa luồng: không bán vượt tồn kho

Test tự động: `InventorySocketConcurrencyTests` (cùng thiết kế dữ liệu với `OrderTransactionIntegrationTests` của thành viên 4). Mỗi test tự tạo dữ liệu riêng (SKU và tài khoản ngẫu nhiên) và tự dọn sau khi chạy. Các luồng được thả chạy cùng lúc bằng `CountDownLatch`, không dùng `Thread.sleep()` để giả lập đồng thời.

| Mã | Kịch bản | Kết quả mong đợi | Kết quả thực tế | Minh chứng |
|---|---|---|---|---|
| ĐT-01 | Kho còn 2; 3 khách (3 luồng) cùng mua 1 chiếc | Đúng 2 đơn thành công, 1 báo hết hàng; tồn kho = 0 (không âm); có đúng 2 đơn trong database; lệnh `CHECK_STOCK` báo `stockQuantity` = 0 và `available` = false | **[ĐIỀN]** | Log `checkout-1..3`, ảnh kết quả test |
| ĐT-02 | Kho còn 5; 20 khách (10 luồng) cùng mua 1 chiếc, đồng thời 1 client Socket liên tục `CHECK_STOCK` | Đúng 5 thành công, 15 hết hàng; tồn kho = 0; mọi giá trị client Socket đọc được nằm trong 0..5 và chỉ giảm, không tăng | **[ĐIỀN]** | Dòng log `[TEST] Client Socket đã quan sát N lần, chuỗi tồn kho: [...]` |
| ĐT-03 | Sau 1 lần checkout, hỏi `CHECK_STOCK` (SKU viết thường) và `GET_ORDER_STATUS` | Tồn kho giảm đúng 1; đơn ở trạng thái `PENDING`, `UNPAID`; không lộ email khách; SKU hoặc mã đơn lạ trả `NOT_FOUND` | **[ĐIỀN]** | Ảnh kết quả test |
| ĐT-04 | 10 client Socket mỗi client gửi 20 lệnh `CHECK_STOCK` đọc database thật | Cả 200 phản hồi đều có số lượng đúng | **[ĐIỀN]** | Ảnh kết quả test |

Cơ chế bảo đảm không vượt tồn kho (nằm ở `OrderService`, thành viên 4): giao dịch `@Transactional` khóa dòng `ProductVariants` bằng `PESSIMISTIC_WRITE`, kiểm tra tồn kho sau khi khóa rồi mới trừ, nên các giao dịch tranh cùng một biến thể phải xếp hàng. Socket Server chỉ đọc dữ liệu đã commit.

## 4.x.3 Kiểm thử mạng: Socket Server và 3 Client

Test tự động không cần database: `ShopSocketServerTests`, `BoundedLineReaderTests` (dùng socket thật trên localhost, dữ liệu tồn kho giả).

| Mã | Kịch bản | Kết quả mong đợi | Test tự động | Kết quả thực tế |
|---|---|---|---|---|
| SK-01 | `PING`, `CHECK_STOCK`, `GET_ORDER_STATUS` | Phản hồi JSON đúng định dạng, tiếng Việt không lỗi font | `ping_returnsPong`, `checkStock_returnsDataIncludingVietnameseText`, `getOrderStatus_returnsStatusWithoutPersonalData` | **[ĐIỀN]** |
| SK-02 | 3 client kết nối cùng lúc | Cả 3 nhận phản hồi, do 3 luồng khác nhau phục vụ | `threeClientsAtTheSameTime_areServedConcurrentlyOnDifferentThreads` (mỗi lần tra cứu chờ đến khi đủ 3 luồng cùng vào, nếu server tuần tự thì test thất bại) | **[ĐIỀN]** |
| SK-03 | 30 client, mỗi client 25 yêu cầu | Đủ 750 phản hồi đúng | `manyClientsSendingManyRequests_allGetAnswers` | **[ĐIỀN]** |
| SK-04 | Lệnh lạ, thiếu tham số, SKU chứa ký tự nguy hiểm (`' OR 1=1 --`, `../`) | Báo lỗi đúng mã, không truy vấn database, kết nối vẫn mở | `unknownCommand_...`, `invalidArguments_areRejectedBeforeTouchingTheDatabase`, `controlCharacters_areRejected` | **[ĐIỀN]** |
| SK-05 | Dòng request quá dài (400 ký tự khi giới hạn 128) | `LINE_TOO_LONG`, đóng kết nối, client khác không ảnh hưởng | `oversizeLine_returnsErrorAndClosesConnection` | **[ĐIỀN]** |
| SK-06 | Client ngắt đột ngột (RST, chưa gửi xong dòng) | Server không chết; kết nối được giải phóng (số client đang phục vụ về 0) | `clientDisconnectsAbruptly_serverKeepsRunningAndReleasesResources` | **[ĐIỀN]** |
| SK-07 | Client im lặng quá thời gian chờ | `TIMEOUT` rồi đóng kết nối | `idleClient_isDisconnectedAfterTimeout` | **[ĐIỀN]** |
| SK-08 | Server đủ số client tối đa | Client mới nhận `BUSY`; khi có chỗ trống lại kết nối được | `whenServerIsFull_newClientGetsBusyAndSlotIsReusedLater` | **[ĐIỀN]** |
| SK-09 | Lỗi bất ngờ khi tra cứu (database lỗi) | Client chỉ nhận `INTERNAL_ERROR`, không lộ chi tiết; kết nối vẫn dùng được | `lookupThrowsException_clientGetsInternalErrorWithoutDetails` | **[ĐIỀN]** |
| SK-10 | Server bật token | Chưa `AUTH` thì `UNAUTHORIZED`; sai token thì `AUTH_FAILED`; đúng token thì dùng được; kết nối mới phải `AUTH` lại | `authToken_isRequiredForBusinessCommands` | **[ĐIỀN]** |
| SK-11 | Tắt server | Đóng toàn bộ kết nối, không còn nhận kết nối mới | `stop_closesEverything` | **[ĐIỀN]** |

## 4.x.4 Demo 3 Client

Thực hiện theo `docs/DEMO_SOCKET_3_CLIENTS.md`. Chèn vào báo cáo:

1. Ảnh 3 cửa sổ client chạy cùng lúc (thấy `handledBy` khác nhau). **[CHÈN ẢNH]**
2. Ảnh log server có 3 tên luồng `shop-socket-worker-1..3`. **[CHÈN ẢNH]**
3. Ảnh client nhập sai lệnh và nhận lỗi, server vẫn chạy. **[CHÈN ẢNH]**
4. Ảnh log khi một client đóng đột ngột. **[CHÈN ẢNH]**

## 4.x.5 Nhận xét và hạn chế

- Socket Server hiện chỉ cho phép **đọc** (kiểm tra kho, xem trạng thái đơn); việc đặt hàng vẫn đi qua website.
- Giao thức dùng TCP thuần, chưa mã hóa; nếu mở ra ngoài mạng nội bộ cần TLS hoặc VPN.
- Test đồng thời chỉ chứng minh trên SQL Server và mức cô lập mặc định của môi trường nhóm; kết quả có thể khác nếu đổi cấu hình khóa.
- Số client tối đa (mặc định 20) và thời gian chờ (60 giây) là giá trị chọn cho đồ án, cần chỉnh nếu triển khai thật.
