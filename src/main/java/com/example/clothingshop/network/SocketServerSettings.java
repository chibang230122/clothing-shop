package com.example.clothingshop.network;

/**
 * Cấu hình của Socket Server (thuần Java, không phụ thuộc Spring để dễ kiểm thử).
 *
 * @param port              cổng lắng nghe (0 = hệ điều hành tự chọn cổng trống, dùng cho test)
 * @param bindAddress       địa chỉ IP lắng nghe. Mặc định 127.0.0.1 (chỉ máy local);
 *                          đặt 0.0.0.0 nếu muốn các máy khác trong mạng LAN kết nối
 * @param maxClients        số client tối đa được phục vụ cùng lúc (kích thước thread pool)
 * @param idleTimeoutMillis thời gian chờ tối đa khi client không gửi gì (0 = không giới hạn)
 * @param maxLineBytes      độ dài tối đa của một dòng request (tính bằng byte)
 */
public record SocketServerSettings(int port,
                                   String bindAddress,
                                   int maxClients,
                                   int idleTimeoutMillis,
                                   int maxLineBytes) {

    public SocketServerSettings {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Cổng không hợp lệ: " + port);
        }
        if (bindAddress == null || bindAddress.isBlank()) {
            throw new IllegalArgumentException("Địa chỉ bind không được để trống.");
        }
        if (maxClients < 1) {
            throw new IllegalArgumentException("maxClients phải >= 1.");
        }
        if (idleTimeoutMillis < 0) {
            throw new IllegalArgumentException("idleTimeoutMillis không được âm.");
        }
        if (maxLineBytes < 16 || maxLineBytes > 65536) {
            throw new IllegalArgumentException("maxLineBytes phải nằm trong khoảng 16..65536.");
        }
    }

    public static SocketServerSettings defaults() {
        return new SocketServerSettings(9099, "127.0.0.1", 20, 60_000, 512);
    }
}
