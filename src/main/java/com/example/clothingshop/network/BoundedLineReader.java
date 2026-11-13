package com.example.clothingshop.network;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Đọc từng dòng (kết thúc bằng \n hoặc \r\n) nhưng giới hạn độ dài.
 * BufferedReader.readLine() không giới hạn độ dài nên client độc hại có thể gửi
 * một dòng cực dài để làm cạn bộ nhớ của server.
 */
final class BoundedLineReader {

    private final InputStream in;
    private final int maxBytes;

    BoundedLineReader(InputStream in, int maxBytes) {
        this.in = new BufferedInputStream(in);
        this.maxBytes = maxBytes;
    }

    /**
     * @return nội dung dòng (đã bỏ ký tự xuống dòng), hoặc null nếu client đã đóng kết nối
     *         mà không còn dữ liệu nào.
     * @throws LineTooLongException khi dòng dài hơn giới hạn
     */
    String readLine() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(64);
        boolean readAnything = false;
        int b;
        while ((b = in.read()) != -1) {
            readAnything = true;
            if (b == '\n') {
                return decode(buffer);
            }
            if (b == '\r') {
                continue;
            }
            if (buffer.size() >= maxBytes) {
                throw new LineTooLongException(maxBytes);
            }
            buffer.write(b);
        }
        // Hết dữ liệu: nếu còn phần dòng chưa có ký tự xuống dòng thì vẫn trả về
        return (readAnything && buffer.size() > 0) ? decode(buffer) : null;
    }

    private static String decode(ByteArrayOutputStream buffer) {
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }
}
