package com.example.clothingshop.network;

/**
 * Kết quả xử lý một request.
 *
 * @param json            dòng JSON gửi lại cho client
 * @param closeConnection true nếu server phải đóng kết nối sau khi gửi (QUIT hoặc lỗi nghiêm trọng)
 * @param command         tên lệnh (dùng cho log)
 * @param status          OK hoặc ERROR (dùng cho log)
 * @param errorCode       mã lỗi, null nếu thành công (dùng cho log)
 */
public record ProtocolReply(String json,
                            boolean closeConnection,
                            String command,
                            String status,
                            String errorCode) {
}
