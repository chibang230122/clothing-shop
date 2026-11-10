package com.example.clothingshop.network;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Phân tích và thực thi từng dòng request theo giao thức của shop (xem docs/SOCKET_PROTOCOL.md).
 * Lớp này không biết gì về Socket nên có thể kiểm thử trực tiếp.
 *
 * Mọi dữ liệu từ client đều bị coi là không tin cậy: kiểm tra ký tự, số lượng tham số và định dạng
 * trước khi truy vấn cơ sở dữ liệu.
 */
public final class CommandProcessor {

    /** SKU: chữ, số, dấu chấm, gạch ngang, gạch dưới; tối đa 50 ký tự (khớp cột SKU VARCHAR(50)). */
    static final Pattern SKU_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{1,50}$");

    /** Mã đơn: chữ, số, gạch ngang, gạch dưới; tối đa 30 ký tự (khớp cột OrderCode VARCHAR(30)). */
    static final Pattern ORDER_CODE_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,30}$");

    private static final Pattern COMMAND_NAME = Pattern.compile("^[A-Z_]{1,20}$");

    private static final List<String> COMMAND_HELP = List.of(
            "PING                      - kiểm tra kết nối",
            "CHECK_STOCK <sku>         - xem tồn kho của một biến thể theo SKU",
            "GET_ORDER_STATUS <code>   - xem trạng thái đơn hàng theo mã đơn",
            "AUTH <token>              - xác thực (chỉ cần khi server bật token)",
            "HELP                      - danh sách lệnh",
            "QUIT                      - đóng kết nối");

    private final StockLookup stockLookup;
    private final OrderStatusLookup orderStatusLookup;
    private final String authToken;

    /**
     * @param authToken token dùng chung giữa server và client. Null hoặc rỗng nghĩa là không yêu cầu xác thực.
     */
    public CommandProcessor(StockLookup stockLookup, OrderStatusLookup orderStatusLookup, String authToken) {
        this.stockLookup = stockLookup;
        this.orderStatusLookup = orderStatusLookup;
        this.authToken = authToken == null ? "" : authToken;
    }

    /** Trạng thái của một kết nối (mỗi kết nối có một Session riêng). */
    public static final class Session {
        private boolean authenticated;

        boolean isAuthenticated() {
            return authenticated;
        }
    }

    public Session newSession() {
        return new Session();
    }

    public boolean isAuthRequired() {
        return !authToken.isEmpty();
    }

    public ProtocolReply process(String rawLine, Session session) {
        try {
            return doProcess(rawLine, session);
        } catch (RuntimeException ex) {
            // Lỗi bất ngờ (ví dụ mất kết nối database): ghi log phía server, không lộ chi tiết cho client.
            SocketLog.error("Lỗi khi xử lý request, client chỉ nhận INTERNAL_ERROR", ex);
            return error("UNKNOWN", "INTERNAL_ERROR", "Server gặp lỗi khi xử lý yêu cầu.", false);
        }
    }

    private ProtocolReply doProcess(String rawLine, Session session) {
        if (rawLine == null || rawLine.isBlank()) {
            return error("UNKNOWN", "EMPTY_REQUEST", "Yêu cầu trống.", false);
        }
        if (containsControlCharacter(rawLine)) {
            return error("UNKNOWN", "INVALID_REQUEST", "Yêu cầu chứa ký tự không hợp lệ.", false);
        }

        String[] tokens = rawLine.trim().split("\\s+");
        String command = tokens[0].toUpperCase(Locale.ROOT);
        if (!COMMAND_NAME.matcher(command).matches()) {
            return error("UNKNOWN", "UNKNOWN_COMMAND", "Lệnh không được hỗ trợ. Gõ HELP để xem danh sách lệnh.", false);
        }

        switch (command) {
            case "PING": {
                ProtocolReply invalid = noArgs(command, tokens);
                return invalid != null ? invalid : ping();
            }
            case "HELP": {
                ProtocolReply invalid = noArgs(command, tokens);
                return invalid != null ? invalid : ok(command, Map.of("commands", COMMAND_HELP));
            }
            case "QUIT": {
                ProtocolReply invalid = noArgs(command, tokens);
                return invalid != null ? invalid : bye();
            }
            case "AUTH":
                return auth(tokens, session);
            case "CHECK_STOCK": {
                ProtocolReply denied = needLogin(command, session);
                return denied != null ? denied : checkStock(tokens);
            }
            case "GET_ORDER_STATUS": {
                ProtocolReply denied = needLogin(command, session);
                return denied != null ? denied : getOrderStatus(tokens);
            }
            default:
                return error("UNKNOWN", "UNKNOWN_COMMAND", "Lệnh không được hỗ trợ. Gõ HELP để xem danh sách lệnh.", false);
        }
    }

    // ===== Các lệnh =====

    private ProtocolReply ping() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", "PONG");
        return ok("PING", data);
    }

    private ProtocolReply bye() {
        ProtocolReply reply = ok("QUIT", Map.of("message", "BYE"));
        return new ProtocolReply(reply.json(), true, reply.command(), reply.status(), null);
    }

    private ProtocolReply auth(String[] tokens, Session session) {
        if (tokens.length != 2) {
            return error("AUTH", "INVALID_ARGUMENT", "Cú pháp: AUTH <token>", false);
        }
        if (!isAuthRequired()) {
            return ok("AUTH", Map.of("message", "Server không yêu cầu xác thực."));
        }
        boolean match = MessageDigest.isEqual(
                tokens[1].getBytes(StandardCharsets.UTF_8),
                authToken.getBytes(StandardCharsets.UTF_8));
        if (!match) {
            return error("AUTH", "AUTH_FAILED", "Token không đúng.", false);
        }
        session.authenticated = true;
        return ok("AUTH", Map.of("message", "Xác thực thành công."));
    }

    private ProtocolReply checkStock(String[] tokens) {
        if (tokens.length != 2) {
            return error("CHECK_STOCK", "INVALID_ARGUMENT", "Cú pháp: CHECK_STOCK <sku>", false);
        }
        String sku = tokens[1];
        if (!SKU_PATTERN.matcher(sku).matches()) {
            return error("CHECK_STOCK", "INVALID_ARGUMENT",
                    "SKU chỉ gồm chữ, số, '.', '-', '_' và tối đa 50 ký tự.", false);
        }
        Optional<StockInfo> found = stockLookup.findStockBySku(sku);
        if (found.isEmpty()) {
            return error("CHECK_STOCK", "NOT_FOUND", "Không tìm thấy SKU " + sku + ".", false);
        }
        StockInfo info = found.get();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sku", info.sku());
        data.put("productName", info.productName());
        data.put("size", info.size());
        data.put("color", info.color());
        data.put("stockQuantity", info.stockQuantity());
        data.put("active", info.active());
        data.put("available", info.available());
        return ok("CHECK_STOCK", data);
    }

    private ProtocolReply getOrderStatus(String[] tokens) {
        if (tokens.length != 2) {
            return error("GET_ORDER_STATUS", "INVALID_ARGUMENT", "Cú pháp: GET_ORDER_STATUS <orderCode>", false);
        }
        String code = tokens[1];
        if (!ORDER_CODE_PATTERN.matcher(code).matches()) {
            return error("GET_ORDER_STATUS", "INVALID_ARGUMENT",
                    "Mã đơn chỉ gồm chữ, số, '-', '_' và tối đa 30 ký tự.", false);
        }
        Optional<OrderStatusInfo> found = orderStatusLookup.findOrderStatus(code);
        if (found.isEmpty()) {
            return error("GET_ORDER_STATUS", "NOT_FOUND", "Không tìm thấy đơn hàng " + code + ".", false);
        }
        OrderStatusInfo info = found.get();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderCode", info.orderCode());
        data.put("orderStatus", info.orderStatus());
        data.put("paymentStatus", info.paymentStatus());
        data.put("orderDate", info.orderDate());
        return ok("GET_ORDER_STATUS", data);
    }

    // ===== Tiện ích =====

    /** Trả về lỗi nếu lệnh không nhận tham số mà client lại gửi kèm; trả về null nếu hợp lệ. */
    private ProtocolReply noArgs(String command, String[] tokens) {
        if (tokens.length != 1) {
            return error(command, "INVALID_ARGUMENT", "Lệnh " + command + " không có tham số.", false);
        }
        return null;
    }

    /** Trả về lỗi UNAUTHORIZED nếu server bật token mà client chưa AUTH; trả về null nếu được phép. */
    private ProtocolReply needLogin(String command, Session session) {
        if (isAuthRequired() && !session.isAuthenticated()) {
            return error(command, "UNAUTHORIZED", "Cần gửi AUTH <token> trước khi dùng lệnh này.", false);
        }
        return null;
    }

    private static boolean containsControlCharacter(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c < 0x20 && c != '\t') || c == 0x7F) {
                return true;
            }
        }
        return false;
    }

    private ProtocolReply ok(String command, Map<String, Object> data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "OK");
        body.put("command", command);
        body.put("handledBy", Thread.currentThread().getName());
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("data", data);
        return new ProtocolReply(Json.write(body), false, command, "OK", null);
    }

    static ProtocolReply error(String command, String code, String message, boolean close) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "ERROR");
        body.put("command", command);
        body.put("handledBy", Thread.currentThread().getName());
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("code", code);
        body.put("message", message);
        return new ProtocolReply(Json.write(body), close, command, "ERROR", code);
    }
}
