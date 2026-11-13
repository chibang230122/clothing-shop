# Giao thức Socket và thiết kế đa luồng (Thành viên 5)

Tài liệu này mô tả Socket Server/Client của shop: loại request, dữ liệu trả về, mã lỗi, các giới hạn an toàn và cách server xử lý nhiều client cùng lúc. Phần 6 dùng làm nội dung cho mục "thiết kế đa luồng và mạng" (tiêu chí 1.5) trong báo cáo.

## 1. Mục đích

Website bán quần áo là ứng dụng Spring Boot. Ngoài giao diện web, shop có một **Socket Server** để các chương trình khác (ví dụ máy kiểm kho, quầy thu ngân) hỏi nhanh:

- Biến thể (SKU) còn bao nhiêu hàng.
- Đơn hàng đang ở trạng thái nào.

Server chỉ **đọc** dữ liệu. Việc trừ kho khi đặt hàng vẫn do `OrderService.checkout` làm trong transaction (khóa `PESSIMISTIC_WRITE`, thành viên 4).

## 2. Kết nối

| Mục | Giá trị |
|---|---|
| Giao thức vận chuyển | TCP |
| Cổng mặc định | `9099` (`shop.socket.port`) |
| Địa chỉ lắng nghe mặc định | `127.0.0.1` (chỉ máy local). Muốn máy khác trong LAN kết nối: đặt biến môi trường `SHOP_SOCKET_BIND_ADDRESS=0.0.0.0` và nên đặt `SHOP_SOCKET_AUTH_TOKEN` |
| Mã hóa ký tự | UTF-8 |
| Phiên làm việc | Một kết nối giữ được nhiều request, mỗi request một dòng |

## 3. Định dạng

### 3.1 Request

Mỗi request là **một dòng văn bản** kết thúc bằng `\n` (hoặc `\r\n`):

```
LỆNH [tham số]
```

- Tên lệnh không phân biệt hoa thường (`ping` = `PING`).
- Tham số cách nhau bằng khoảng trắng.
- Dòng dài tối đa **512 byte** (`shop.socket.max-line-length`).

### 3.2 Response

Mỗi response là **một dòng JSON** (UTF-8).

Thành công:

```json
{"status":"OK","command":"CHECK_STOCK","handledBy":"shop-socket-worker-2","timestamp":"2026-11-10T09:30:00.123+07:00","data":{ ... }}
```

Lỗi:

```json
{"status":"ERROR","command":"CHECK_STOCK","handledBy":"shop-socket-worker-2","timestamp":"2026-11-10T09:30:00.123+07:00","code":"NOT_FOUND","message":"Không tìm thấy SKU ABC."}
```

| Trường | Ý nghĩa |
|---|---|
| `status` | `OK` hoặc `ERROR` |
| `command` | Tên lệnh server hiểu được; `UNKNOWN` nếu không nhận ra |
| `handledBy` | Tên luồng của server đã xử lý (dùng để chứng minh nhiều client được phục vụ bởi nhiều luồng) |
| `timestamp` | Thời điểm server trả lời |
| `data` | Dữ liệu kết quả (chỉ khi `OK`) |
| `code`, `message` | Mã lỗi và mô tả (chỉ khi `ERROR`) |

## 4. Các lệnh

### `PING`
Kiểm tra kết nối. Không có tham số.

```
> PING
< {"status":"OK","command":"PING",...,"data":{"message":"PONG"}}
```

### `CHECK_STOCK <sku>`
Xem tồn kho của một biến thể. `sku` chỉ gồm chữ, số, `.`, `-`, `_`, tối đa 50 ký tự; không phân biệt hoa thường.

```
> CHECK_STOCK SKU-001-M-BLK
< {"status":"OK","command":"CHECK_STOCK",...,"data":{"sku":"SKU-001-M-BLK","productName":"...","size":"M","color":"Đen","stockQuantity":14,"active":true,"available":true}}
```

| Trường `data` | Ý nghĩa |
|---|---|
| `stockQuantity` | Số lượng tồn trong database (không bao giờ âm) |
| `active` | Biến thể và sản phẩm cha đều đang kinh doanh |
| `available` | `true` khi `active` và `stockQuantity > 0` |

### `GET_ORDER_STATUS <orderCode>`
Xem trạng thái đơn. `orderCode` chỉ gồm chữ, số, `-`, `_`, tối đa 30 ký tự. **Không** trả tên, số điện thoại hay địa chỉ khách.

```
> GET_ORDER_STATUS ORD-0001
< {"status":"OK","command":"GET_ORDER_STATUS",...,"data":{"orderCode":"ORD-0001","orderStatus":"PENDING","paymentStatus":"UNPAID","orderDate":"2026-11-10T09:30:00"}}
```

### `AUTH <token>`
Chỉ cần khi server được cấu hình `SHOP_SOCKET_AUTH_TOKEN`. Phải gửi `AUTH` thành công trước khi dùng `CHECK_STOCK` và `GET_ORDER_STATUS`; `PING`, `HELP`, `QUIT` luôn dùng được. Mỗi kết nối phải `AUTH` riêng.

### `HELP`
Trả danh sách lệnh.

### `QUIT`
Server trả `BYE` rồi đóng kết nối.

## 5. Mã lỗi

| `code` | Khi nào | Server đóng kết nối? |
|---|---|---|
| `EMPTY_REQUEST` | Dòng trống | Không |
| `INVALID_REQUEST` | Chứa ký tự điều khiển | Không |
| `UNKNOWN_COMMAND` | Lệnh không được hỗ trợ | Không |
| `INVALID_ARGUMENT` | Thiếu/thừa tham số, SKU hoặc mã đơn sai định dạng | Không |
| `NOT_FOUND` | Không có SKU hoặc đơn hàng đó | Không |
| `UNAUTHORIZED` | Server bật token mà client chưa `AUTH` | Không |
| `AUTH_FAILED` | Token sai | Không |
| `INTERNAL_ERROR` | Lỗi bất ngờ phía server (ví dụ mất kết nối database); chi tiết chỉ ghi trong log của server | Không |
| `LINE_TOO_LONG` | Dòng request dài hơn giới hạn | **Có** (không thể đồng bộ lại dòng) |
| `TIMEOUT` | Quá `shop.socket.idle-timeout-ms` (mặc định 60 giây) không gửi gì | **Có** |
| `BUSY` | Đã đủ `shop.socket.max-clients` (mặc định 20) client | **Có** |

## 6. Thiết kế đa luồng và mạng (nội dung cho báo cáo mục 1.5)

### 6.1 Sơ đồ

```
 Client A ──┐                       ┌─ shop-socket-worker-1 ─ ClientHandler(A) ─┐
 Client B ──┼─► ServerSocket.accept ┼─ shop-socket-worker-2 ─ ClientHandler(B) ─┼─► CommandProcessor
 Client C ──┘   (luồng acceptor)    └─ shop-socket-worker-3 ─ ClientHandler(C) ─┘        │
                                       ThreadPoolExecutor (tối đa 20)                    ▼
                                                                         InventoryService / OrderStatusService
                                                                       (transaction chỉ-đọc ─► SQL Server)
```

### 6.2 Các lớp

| Lớp | Vai trò |
|---|---|
| `ShopSocketServer` | Mở `ServerSocket`, chạy luồng acceptor, quản lý thread pool, đóng tài nguyên khi dừng |
| `ClientHandler` | Phục vụ **một** client trên **một** luồng: đọc dòng, xử lý, trả lời, log, đóng socket |
| `BoundedLineReader` | Đọc từng dòng có giới hạn độ dài |
| `CommandProcessor` | Kiểm tra dữ liệu và thực thi lệnh; không biết gì về socket nên kiểm thử trực tiếp được |
| `InventoryService`, `OrderStatusService` | Đọc SQL Server trong `@Transactional(readOnly = true)` |
| `ShopSocketServerLifecycle` | Gắn server vào vòng đời Spring: tự chạy khi website khởi động, tự đóng khi tắt |
| `socket-client/ShopSocketClient` | Chương trình client độc lập (không Spring, không database) |

### 6.3 Vì sao chọn mô hình này

- **Luồng acceptor chỉ làm `accept()`**, mọi việc chậm (đọc mạng, truy vấn database) nằm ở luồng worker, nên một client chậm không chặn client khác.
- **Thread pool có giới hạn** (`ThreadPoolExecutor` với `SynchronousQueue`): không tạo luồng vô hạn. Khi đủ `max-clients`, kết nối mới nhận `BUSY` rồi bị đóng thay vì làm server cạn tài nguyên.
- **Mỗi kết nối có `Session` riêng** (trạng thái đã `AUTH` hay chưa), không có dữ liệu thay đổi được dùng chung giữa các luồng, nên không cần khóa trong lớp mạng.
- **Server chỉ đọc** nên không thể làm sai tồn kho. Việc ghi tồn kho chỉ nằm ở `OrderService` và `OrderCancellationService`, nơi dùng `PESSIMISTIC_WRITE` để hai khách không thể cùng mua món hàng cuối. Vì chỉ đọc dữ liệu đã commit, số `stockQuantity` client nhận được không bao giờ âm và không phản ánh giao dịch đang dở.

### 6.4 Xử lý lỗi và đóng tài nguyên

| Tình huống | Cách xử lý |
|---|---|
| Client ngắt đột ngột hoặc mất mạng | `IOException` được bắt trong `ClientHandler`; socket, stream được đóng trong `try-with-resources`; client được gỡ khỏi danh sách đang phục vụ; log ghi `mất kết nối`; các client khác không ảnh hưởng |
| Client im lặng quá lâu | `SO_TIMEOUT`: server trả `TIMEOUT` rồi đóng, giải phóng luồng worker |
| Client gửi dòng cực dài | `BoundedLineReader` dừng đọc ở 512 byte, trả `LINE_TOO_LONG`, đóng kết nối |
| Client gửi lệnh/tham số sai | `CommandProcessor` kiểm tra trước, **không truy vấn database** với dữ liệu sai định dạng; kết nối vẫn mở |
| Database lỗi | `CommandProcessor` bắt lỗi, client chỉ nhận `INTERNAL_ERROR`, chi tiết ghi vào log server |
| Tắt website | `ShopSocketServerLifecycle.stop()` đóng `ServerSocket`, đóng các kết nối đang mở, `shutdown()` thread pool, chờ tối đa 5 giây rồi `shutdownNow()` |
| Không mở được cổng | Chỉ ghi log lỗi, website vẫn chạy bình thường |

### 6.5 Bảo mật

- Mặc định chỉ lắng nghe `127.0.0.1`. Khi mở ra mạng ngoài mà chưa đặt token, server ghi cảnh báo trong log.
- Token (nếu dùng) lấy từ biến môi trường, so sánh bằng `MessageDigest.isEqual` (thời gian không phụ thuộc nội dung), và bị che thành `AUTH ***` trong log.
- Mọi dữ liệu từ client được coi là không tin cậy: giới hạn độ dài, loại bỏ ký tự điều khiển, kiểm tra SKU và mã đơn bằng biểu thức chính quy, rồi mới truy vấn bằng Spring Data (tham số hóa).
- `GET_ORDER_STATUS` không trả dữ liệu cá nhân.
- Giao thức hiện **không mã hóa** (TCP thuần). Nếu dùng ngoài mạng nội bộ thì cần TLS hoặc đặt sau VPN.

## 7. Log

Mỗi request ghi một dòng có thời điểm (do Logback thêm vào), tên luồng, địa chỉ client, lệnh, kết quả và thời gian xử lý:

```
INFO: thread=shop-socket-worker-2 client=/127.0.0.1:58336 request="CHECK_STOCK SKU-001-M-BLK" status=OK took=1ms
INFO: thread=shop-socket-worker-4 client=/127.0.0.1:58346 request="CHECK_STOCK KHONG-CO" status=ERROR(NOT_FOUND) took=2ms
```

Log cũng được ghi vào `logs/clothing-shop.log` (thư mục `logs/` nằm trong `.gitignore`, không commit lên Git).

## 8. Cấu hình

| Khóa | Mặc định | Ý nghĩa |
|---|---|---|
| `shop.socket.enabled` | `true` | Bật/tắt Socket Server |
| `shop.socket.port` | `9099` | Cổng lắng nghe (0 = hệ điều hành tự chọn, dùng cho test) |
| `shop.socket.bind-address` | `127.0.0.1` | Địa chỉ lắng nghe (biến môi trường `SHOP_SOCKET_BIND_ADDRESS`) |
| `shop.socket.max-clients` | `20` | Số client phục vụ cùng lúc |
| `shop.socket.idle-timeout-ms` | `60000` | Thời gian chờ khi client im lặng |
| `shop.socket.max-line-length` | `512` | Độ dài tối đa một dòng request (byte) |
| `shop.socket.auth-token` | (rỗng) | Token xác thực (biến môi trường `SHOP_SOCKET_AUTH_TOKEN`) |
