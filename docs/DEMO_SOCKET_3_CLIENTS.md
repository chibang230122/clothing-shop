# Kịch bản demo Socket Server với 3 Client (Thành viên 5)

Thời lượng gợi ý: 4 đến 5 phút trong buổi demo trực tiếp (hoặc quay video).

## 1. Chuẩn bị (làm trước buổi demo)

1. SQL Server đã chạy, database `ClothingShopDB` đã tạo bằng `database/ClothingShopDB.sql`.
2. Chạy hai truy vấn để chọn dữ liệu demo:

   ```sql
   SELECT TOP 5 SKU, StockQuantity FROM ProductVariants ORDER BY VariantId;
   SELECT TOP 3 OrderCode, OrderStatus FROM Orders ORDER BY OrderId;
   ```

   Ghi lại một SKU (ví dụ `SKU-001-M-BLK`) và một mã đơn (ví dụ `ORD-0001`). Nếu khác, thay vào các lệnh bên dưới.
3. Build Socket Client một lần:

   ```
   cd socket-client
   mvn package
   ```

   Kết quả: `socket-client/target/shop-socket-client-1.0.0.jar` (hoặc chạy `scripts\build-client.bat`).
4. Windows: nếu console hiển thị sai tiếng Việt, gõ `chcp 65001` trước khi chạy client.

## 2. Các bước demo

### Bước 1: Khởi động server
Chạy `ClothingShopApplication` (hoặc `scripts\run-server.bat`). Chỉ cho khán giả thấy dòng log:

```
Socket server đang chạy tại 127.0.0.1:9099 (tối đa 20 client, timeout 60000 ms, dòng tối đa 512 byte)
```

### Bước 2: Mở 3 client riêng
Mở **3 cửa sổ terminal** (3 tiến trình Java riêng) và chạy:

```
java -jar socket-client/target/shop-socket-client-1.0.0.jar --name A --repeat 10 --interval 500 CHECK_STOCK SKU-001-M-BLK
java -jar socket-client/target/shop-socket-client-1.0.0.jar --name B --repeat 10 --interval 700 CHECK_STOCK SKU-001-M-BLK
java -jar socket-client/target/shop-socket-client-1.0.0.jar --name C --repeat 10 --interval 600 GET_ORDER_STATUS ORD-0001
```

Hoặc chạy một lệnh: `scripts\demo-3-clients.bat SKU-001-M-BLK ORD-0001` (Windows, mở 3 cửa sổ) / `scripts/demo-3-clients.sh` (Linux, macOS).

**Điều cần chỉ ra:** cả 3 client nhận phản hồi cùng lúc; trường `handledBy` của 3 client là 3 luồng khác nhau (`shop-socket-worker-1`, `-2`, `-3`).

### Bước 3: Xem log phía server
Trong console của website, mỗi request có một dòng: thời điểm, tên luồng, địa chỉ client, lệnh, kết quả, thời gian xử lý. Đây là minh chứng server xử lý song song.

### Bước 4: Client gửi dữ liệu sai, server vẫn sống
Mở thêm một client tương tác (không có lệnh):

```
java -jar socket-client/target/shop-socket-client-1.0.0.jar --name D
```

Gõ lần lượt:

```
DROP TABLE Users              -> UNKNOWN_COMMAND
CHECK_STOCK                   -> INVALID_ARGUMENT
CHECK_STOCK ' OR 1=1 --       -> INVALID_ARGUMENT (bị chặn trước khi chạm database)
CHECK_STOCK KHONG-TON-TAI     -> NOT_FOUND
PING                          -> PONG (kết nối vẫn mở)
```

Ba client A, B, C vẫn chạy bình thường.

### Bước 5: Client đóng đột ngột
Trong cửa sổ của client D nhấn `Ctrl + C` (hoặc đóng cửa sổ) khi chưa gõ `exit`. Chỉ ra:

- Log server ghi `client đã đóng kết nối` hoặc `mất kết nối`, rồi dòng `CLOSED`.
- Client A, B, C và server vẫn hoạt động; mở client mới vẫn kết nối được.

### Bước 6: Tồn kho thay đổi khi có đơn hàng thật
Giữ một client lặp lại `CHECK_STOCK` (ví dụ `--repeat 30 --interval 1000`). Trên website (cổng 8081) đăng nhập khách hàng, đặt mua đúng SKU đó. Client sẽ thấy `stockQuantity` giảm đúng số lượng đã mua.

### Bước 7: Kiểm thử đồng thời (không bán vượt tồn kho)
Chạy:

```
mvnw test -Dtest=InventorySocketConcurrencyTests
```

Chỉ ra phần log in ra: nhiều luồng `checkout-N` cùng `START`, rồi `SUCCESS` hoặc `OUT_OF_STOCK`, và các test kết thúc `PASSED`. Kịch bản chính: kho còn 2, 3 khách mua cùng lúc, đúng 2 thành công, kho về 0, không ra số âm.

## 3. Danh sách kiểm tra trước giờ demo

- [ ] Website khởi động, thấy dòng `Socket server đang chạy`.
- [ ] Đã có file jar của client.
- [ ] Biết sẵn một SKU còn hàng và một mã đơn có thật.
- [ ] Đã thử bước 2 đến 5 ít nhất một lần.
- [ ] Cổng 9099 không bị chương trình khác dùng.
- [ ] Nếu demo trên 2 máy: server đặt `SHOP_SOCKET_BIND_ADDRESS=0.0.0.0`, `SHOP_SOCKET_AUTH_TOKEN=<token>`, mở cổng 9099 ở tường lửa; client chạy với `--host <IP máy server> --token <token>`.

## 4. Xử lý sự cố nhanh

| Hiện tượng | Nguyên nhân thường gặp | Cách xử lý |
|---|---|---|
| Log có `Không thể khởi động Socket server` | Cổng 9099 đang bị dùng | Tắt chương trình đang dùng cổng hoặc đổi `shop.socket.port` |
| Client báo `Connection refused` | Website chưa chạy hoặc sai cổng | Kiểm tra dòng log khởi động, kiểm tra `--port` |
| Client báo `BUSY` | Đã đủ 20 client | Tắt bớt client hoặc tăng `shop.socket.max-clients` |
| Client báo `UNAUTHORIZED` | Server có token mà client chưa gửi | Thêm `--token` hoặc đặt biến `SHOP_SOCKET_AUTH_TOKEN` |
| Client báo `TIMEOUT` | Để im quá 60 giây | Kết nối lại |
| Tiếng Việt bị lỗi font | Console không dùng UTF-8 | Windows: `chcp 65001` |
| `NOT_FOUND` với SKU có trong database | Sai SKU hoặc database đang dùng khác | Chạy lại truy vấn ở mục 1 |
