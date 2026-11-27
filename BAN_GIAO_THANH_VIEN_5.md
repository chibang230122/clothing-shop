# BÀN GIAO PHẦN VIỆC THÀNH VIÊN 5 — SOCKET SERVER V2, REALTIME CHAT & SWING CLIENT

## 1. Tổng quan chức năng đã hoàn thiện
- **Socket Server V2 (`port 9100`, JSON-Line protocol):** Chạy song song trên Thread Pool riêng biệt, giữ nguyên toàn bộ giao thức V1 cũ để không ảnh hưởng bài làm của các thành viên khác.
- **Cầu nối WebSocket (`/ws/tcp-bridge`):** Kết nối trực tiếp chat bubble trên giao diện Web khách hàng (`chat-bridge.js`, `chat-bubble.js`) với TCP Socket Server, hỗ trợ heartbeat `PING` giữ phiên và tự kết nối lại khi rớt mạng.
- **Chat hỗ trợ khách hàng & Kênh nội bộ (`INTERNAL`):**
  - Nhiều `STAFF` / `ADMIN` cùng theo dõi hàng đợi hỗ trợ, nhận xử lý (`CLAIM_CHAT`) và trao đổi trực tiếp với khách theo thời gian thực.
  - Tạo kênh chat nội bộ giữa các nhân viên, mời thành viên (`ADD_INTERNAL_MEMBER`) và xem danh sách thành viên kênh.
- **Bộ lọc từ cấm đa tầng (`BannedWordFilterService`):** Chuẩn hóa Unicode (bỏ dấu, ký tự thay thế `@`, `0`, `1`, `3`, `$`), khớp ranh giới từ (`word-boundary`), hỗ trợ 2 chế độ **`MASK`** (che `***`) và **`BLOCK`** (chặn gửi), kèm lưu lịch sử vi phạm cho Admin.
- **Quản lý Sản phẩm & Tồn kho thời gian thực (`ShopManagerSwingApp`):**
  - Ứng dụng Desktop Java Swing độc lập trong module `socket-client`.
  - Hỗ trợ khóa lạc quan (`@Version` — phát hiện xung đột sửa đồng thời) và khóa bi quan (`PESSIMISTIC_WRITE` — đảm bảo an toàn khi trừ/nhập kho).

---

## 2. Hướng dẫn cài đặt Cơ sở dữ liệu (Chỉ 2 bước)
Mở **SQL Server Management Studio (SSMS)** và chạy tuần tự 2 file trong thư mục `database\`:
1. **`database\ClothingShopDB.sql`** *(nếu máy chưa có database gốc của nhóm)*.
2. **`database\V2_Realtime_Full_Setup.sql`** *(chạy 1 lần duy nhất — đã gộp toàn bộ bảng Chat V2, bảng lịch sử tồn kho, 100 từ cấm chuẩn UTF-8 và 4 tài khoản test; hỗ trợ chạy lại nhiều lần không trùng dữ liệu)*.

### Danh sách tài khoản test mặc định (Mật khẩu chung: `AdminShop123@`)
| Vai trò | Email đăng nhập | Ghi chú |
| :--- | :--- | :--- |
| **ADMIN** | `admin@clothingshop.local` | Toàn quyền quản trị sản phẩm, kho, chat & từ cấm |
| **STAFF 1** | `staff@clothingshop.local` | Nhân viên hỗ trợ & quản lý kho |
| **STAFF 2** | `staff2@clothingshop.local` | Nhân viên Bán hàng 2 (test chat nhiều nhân viên) |
| **STAFF 3** | `staff3@clothingshop.local` | Nhân viên Kho 3 (test chat nhiều nhân viên) |

---

## 3. Hướng dẫn chạy hệ thống

### Bước 1: Khởi động Web Server & Socket Server (Port `8080` và `9100`)
```powershell
.\mvnw.cmd spring-boot:run
```
- Truy cập Web khách hàng tại: `http://localhost:8080` (bong bóng chat ở góc phải màn hình).

### Bước 2: Đóng gói và chạy ứng dụng Quản lý Java Swing
```powershell
.\mvnw.cmd -f "socket-client\pom.xml" clean package -DskipTests
java -Dfile.encoding=UTF-8 -jar "socket-client\target\shop-socket-client-1.0.0.jar"
```
- Trên giao diện Swing: chọn sẵn tài khoản ở mục **Tài khoản lưu** $\rightarrow$ bấm **Kết nối** (`127.0.0.1:9100`) $\rightarrow$ bấm **Đăng nhập**.