# Bàn giao Thành viên 5: Đa luồng, Socket Client/Server, kiểm thử

Bàn giao theo mẫu "Quy tắc bàn giao code" của kế hoạch đồ án. Gồm cả 3 đợt (giao thức và skeleton, nhiều client và xử lý lỗi, tích hợp nghiệp vụ và test đồng thời).

Nhánh Git đề xuất: `feature/socket-concurrency` (**[ĐIỀN]** commit ID sau khi commit).

## 1. File đã thêm

Đường dẫn tính từ thư mục gốc của repo (cùng cấp với `pom.xml`).

**Socket Server (mã nguồn chính)**

| File | Vai trò |
|---|---|
| `src/main/java/com/example/clothingshop/network/ShopSocketServer.java` | `ServerSocket`, luồng acceptor, thread pool có giới hạn, từ chối `BUSY`, dừng và đóng tài nguyên |
| `.../network/ClientHandler.java` | Phục vụ từng client, timeout, bắt lỗi ngắt kết nối, log |
| `.../network/CommandProcessor.java` | Kiểm tra dữ liệu và thực thi `PING`, `CHECK_STOCK`, `GET_ORDER_STATUS`, `AUTH`, `HELP`, `QUIT` |
| `.../network/BoundedLineReader.java`, `LineTooLongException.java` | Đọc dòng có giới hạn độ dài |
| `.../network/Json.java`, `ProtocolReply.java`, `SocketLog.java`, `SocketServerSettings.java` | Hỗ trợ (JSON, kết quả, log, cấu hình) |
| `.../network/StockLookup.java`, `StockInfo.java`, `OrderStatusLookup.java`, `OrderStatusInfo.java` | Giao diện và dữ liệu tra cứu (giúp test không cần database) |
| `.../network/ShopSocketProperties.java`, `ShopSocketServerLifecycle.java` | Đọc cấu hình `shop.socket.*`; tự chạy/tắt cùng website |
| `.../concurrency/InventoryService.java` | Tra cứu tồn kho (transaction chỉ-đọc) |
| `.../service/OrderStatusService.java` | Tra cứu trạng thái đơn (transaction chỉ-đọc, không trả dữ liệu cá nhân) |

**Socket Client (chương trình riêng, không Spring)**

| File | Vai trò |
|---|---|
| `socket-client/pom.xml` | Build ra `shop-socket-client-1.0.0.jar` |
| `socket-client/src/main/java/shopclient/ShopSocketClient.java` | Chế độ tương tác và chế độ một lần, `--repeat`, `--token`, `--name` |

**Kiểm thử**

| File | Cần database? |
|---|---|
| `src/test/java/.../network/ShopSocketServerTests.java` (19 test) | Không |
| `src/test/java/.../network/BoundedLineReaderTests.java` (5 test) | Không |
| `src/test/java/.../concurrency/InventorySocketConcurrencyTests.java` (4 test) | **Có**, SQL Server như `OrderTransactionIntegrationTests` |
| `src/test/java/.../concurrency/OrderStockConcurrencyTests.java` (5 test, bổ sung cho Deadline 2: hủy/xác nhận đồng thời, trạng thái đơn qua Socket, chặn ADMIN/STAFF đặt hàng) | **Có**, SQL Server |

**Tài liệu và script**

| File | Nội dung |
|---|---|
| `docs/SOCKET_PROTOCOL.md` | Giao thức, mã lỗi, thiết kế đa luồng (nội dung cho tiêu chí 1.5) |
| `docs/DEMO_SOCKET_3_CLIENTS.md` | Kịch bản demo 3 client, danh sách kiểm tra, xử lý sự cố |
| `docs/BAO_CAO_CHUONG4_DA_LUONG_MANG.md` | Bản nháp mục kiểm thử đa luồng/mạng cho Chương 4 (có ô **[ĐIỀN]** chờ kết quả thật) |
| `scripts/run-server.bat`, `build-client.bat`, `demo-3-clients.bat`, `demo-3-clients.sh` | Chạy server, build client, mở 3 client cùng lúc |

## 2. File đã sửa (chỉ thêm, không đổi dòng cũ)

| File | Thay đổi |
|---|---|
| `src/main/resources/application.properties` | Thêm khối `shop.socket.*` và `logging.file.name=logs/clothing-shop.log` ở cuối file |
| `.gitignore` | Thêm `logs/`, `*.log`, `socket-client/target/` |
| `README.md` | Thêm mục "9. Socket Server và Socket Client" |

Không sửa file nào của thành viên khác. Nếu đã có thay đổi mới ở các file trên, hãy **dán phần thêm vào** thay vì chép đè cả file.

## 3. Chức năng đã hoàn thành (đối chiếu 14 việc trong kế hoạch)

| # | Việc | Trạng thái |
|---|---|---|
| 1 | Tài liệu giao thức | Xong (`docs/SOCKET_PROTOCOL.md`) |
| 2 | `ShopSocketServer` dùng `ServerSocket` | Xong |
| 3 | Thread pool có giới hạn | Xong (`ThreadPoolExecutor`, `SynchronousQueue`, tối đa 20 client, hết chỗ thì `BUSY`) |
| 4 | `ClientHandler` | Xong |
| 5 | `ShopSocketClient` tách biệt, chạy nhiều bản | Xong |
| 6 | Lệnh nghiệp vụ | Xong: `CHECK_STOCK`, `GET_ORDER_STATUS` (kèm `PING`, `AUTH`, `HELP`, `QUIT`) |
| 7 | Kiểm tra dữ liệu, giới hạn kích thước | Xong (độ dài dòng, ký tự điều khiển, biểu thức chính quy cho SKU và mã đơn) |
| 8 | Phục vụ ít nhất 3 client đồng thời | Xong, có test chứng minh |
| 9 | Ngắt kết nối, timeout, đóng tài nguyên | Xong, có test |
| 10 | Phối hợp thành viên 4 thiết kế test tồn kho | `OrderStockConcurrencyTests` viết xong (TK-01 đến TK-05); **cần thành viên 4 xem lại** (mục 6) |
| 11 | Test nhiều luồng chứng minh không vượt tồn kho | `InventorySocketConcurrencyTests` đã chạy đạt 4/4 trên SQL Server ngày 10/11/2026; **cần chạy lại trên code cuối**. `OrderStockConcurrencyTests` **chưa chạy** |
| 12 | Log có thời điểm, client, luồng, kết quả | Xong |
| 13 | Phần kiểm thử Chương 4 | Bản nháp; chờ điền kết quả thật và ảnh |
| 14 | Kịch bản demo 3 client | Xong (`docs/DEMO_SOCKET_3_CLIENTS.md`) |

## 3b. Dữ liệu test dùng để demo

Dùng dữ liệu mẫu của `database/ClothingShopDB.sql`: SKU dạng `SKU-001-M-BLK`, mã đơn dạng `ORD-0001`. Hãy chạy hai truy vấn ở mục 1 của `docs/DEMO_SOCKET_3_CLIENTS.md` để xác nhận trên database của nhóm.

## 4. Cách chạy

```
# Website + Socket Server (cổng 9099)
mvnw spring-boot:run          # hoặc chạy ClothingShopApplication trong IntelliJ

# Build và dùng client
.\mvnw.cmd -f socket-client\pom.xml package
java -jar target/shop-socket-client-1.0.0.jar CHECK_STOCK SKU-001-M-BLK

# Test
mvnw test                                                  # tất cả
mvnw test -Dtest="ShopSocketServerTests,BoundedLineReaderTests"   # không cần database
mvnw test -Dtest="InventorySocketConcurrencyTests,OrderStockConcurrencyTests"   # cần SQL Server
scripts\run-member5-tests.bat                                # chạy 4 lớp test và lưu kết quả vào logs\evidence
```

## 5. Kết quả đã kiểm tra và bằng chứng

Môi trường kiểm tra của thành viên 5 **không có Maven, không có SQL Server**, nên phần dưới là những gì đã chạy thật và những gì chưa.

**Đã chạy và đạt**

- Biên dịch toàn bộ lớp lõi `network/` (JDK 21).
- Biên dịch các lớp dùng Spring (`ShopSocketServerLifecycle`, `ShopSocketProperties`, `InventoryService`, `OrderStatusService`) và `InventorySocketConcurrencyTests` khi đối chiếu với **bản giả lập** của thư viện Spring/JPA (không phải build Maven thật).
- **24/24 test đơn vị đạt** (`ShopSocketServerTests` 19, `BoundedLineReaderTests` 5), chạy bằng bộ chạy JUnit tự viết thay cho Maven. Bao gồm: 3 client đồng thời trên 3 luồng khác nhau, 30 client × 25 yêu cầu, client ngắt đột ngột, timeout, `BUSY`, dòng quá dài, dữ liệu độc hại, lỗi bất ngờ, token, tắt server.
- Chạy thử toàn bộ chuỗi server cùng 3 chương trình client thật với dữ liệu giả: cả 3 client nhận phản hồi do `shop-socket-worker-1`, `-2`, `-3` xử lý; lệnh sai, SKU độc hại và `NOT_FOUND` đều trả lỗi đúng mã, client thoát với mã 1; client nối vào cổng không có server báo lỗi kết nối và thoát mã 1.

**Chưa chạy (cần nhóm làm)**

- `mvnw test` thật trên máy nhóm (nên chạy toàn bộ để chắc các test của người khác vẫn đạt).
- `OrderStockConcurrencyTests` với SQL Server (mới viết, chưa chạy lần nào).
- Demo trên website thật và bước 6 trong kịch bản (đặt hàng khi client đang theo dõi).
- Chưa có người khác review.

## 6. Việc cần phối hợp và lưu ý

1. **Thành viên 4:** xem `InventorySocketConcurrencyTests` (dùng cùng cách tạo dữ liệu và cùng chuỗi lỗi `không đủ số lượng` với `OrderTransactionIntegrationTests`). Nếu đổi nội dung thông báo hết hàng trong `OrderService`, phải sửa hằng `OUT_OF_STOCK_MESSAGE` trong test này.
2. **Mọi `@SpringBootTest` đều khởi động Socket Server** trên cổng 9099 (theo `application.properties`). Nếu cổng đang bận (đang chạy website cùng lúc với test), chỉ có dòng log lỗi, test vẫn chạy. Test của thành viên 5 dùng cổng ngẫu nhiên nên không đụng. Muốn tắt hẳn trong test khác, thêm `properties = "shop.socket.enabled=false"` vào `@SpringBootTest`.
3. **Bảo mật cấu hình (ngoài phạm vi của thành viên 5):** `application.properties` đang chứa mật khẩu SQL Server và mật khẩu admin dạng văn bản trong Git, trái với mục 9.2 của kế hoạch ("Không để mật khẩu thật trong Git"). Nên chuyển sang biến môi trường (`${DB_PASSWORD:}`...) như đã làm với `SHOP_SOCKET_AUTH_TOKEN`, và đổi mật khẩu đã lộ.
4. Một số test mạng dựa vào thời gian (ví dụ chờ tối đa 3 giây để kết nối được giải phóng). Trên máy rất chậm có thể cần tăng thời gian chờ.
5. Giao thức là TCP thuần, chưa mã hóa. Mặc định chỉ lắng nghe `127.0.0.1`; nếu demo giữa hai máy phải đặt `SHOP_SOCKET_BIND_ADDRESS=0.0.0.0` và `SHOP_SOCKET_AUTH_TOKEN`.
6. Việc **ghi** tồn kho không nằm trong Socket Server; nếu sau này thêm lệnh đặt hàng qua socket thì phải đi qua `OrderService.checkout` để giữ cơ chế khóa.
