package com.example.clothingshop.network;

/**
 * Ghi log của phần Socket. Dùng System.Logger nên không cần thêm thư viện;
 * khi chạy trong Spring Boot, log được chuyển qua Logback cùng định dạng có thời điểm và tên luồng.
 */
final class SocketLog {

    private static final System.Logger LOG = System.getLogger("com.example.clothingshop.network.socket");

    private SocketLog() {
    }

    static void info(String format, Object... args) {
        LOG.log(System.Logger.Level.INFO, String.format(format, args));
    }

    static void warn(String format, Object... args) {
        LOG.log(System.Logger.Level.WARNING, String.format(format, args));
    }

    static void error(String message, Throwable error) {
        LOG.log(System.Logger.Level.ERROR, message, error);
    }

    /** Thay ký tự điều khiển và cắt ngắn để dòng log không bị giả mạo hoặc quá dài. */
    static String clean(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String cut = text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
        StringBuilder sb = new StringBuilder(cut.length());
        for (int i = 0; i < cut.length(); i++) {
            char c = cut.charAt(i);
            sb.append(c < 0x20 || c == 0x7F ? '?' : c);
        }
        return sb.toString();
    }
}
