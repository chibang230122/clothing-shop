package shopclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Socket Client của shop: kết nối tới Socket Server, gửi lệnh và in kết quả.
 *
 * Hai chế độ:
 *   1) Tương tác  : java -jar shop-socket-client.jar            (gõ lệnh, Enter để gửi, "exit" để thoát)
 *   2) Một lần    : java -jar shop-socket-client.jar CHECK_STOCK AT-DEN-M
 *
 * Tùy chọn:
 *   --host <ip>        địa chỉ server            (mặc định 127.0.0.1)
 *   --port <cổng>      cổng server               (mặc định 9099)
 *   --token <token>    token AUTH nếu server yêu cầu (hoặc đặt biến môi trường SHOP_SOCKET_AUTH_TOKEN)
 *   --timeout <ms>     thời gian chờ phản hồi    (mặc định 10000)
 *   --repeat <n>       gửi lệnh n lần            (chỉ chế độ một lần, mặc định 1)
 *   --interval <ms>    nghỉ giữa các lần gửi     (mặc định 1000)
 *   --name <tên>       nhãn hiển thị trước mỗi dòng, giúp phân biệt khi chạy nhiều client
 *
 * Mã thoát: 0 = thành công, 1 = lỗi kết nối hoặc server báo lỗi, 2 = sai cú pháp chạy.
 */
public final class ShopSocketClient {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final String host;
    private final int port;
    private final String token;
    private final int timeoutMillis;
    private final String label;
    private final PrintStream out;

    private ShopSocketClient(String host, int port, String token, int timeoutMillis, String label) {
        this.host = host;
        this.port = port;
        this.token = token;
        this.timeoutMillis = timeoutMillis;
        this.label = label;
        this.out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
    }

    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 9099;
        int timeout = 10_000;
        int repeat = 1;
        int interval = 1000;
        String token = System.getenv("SHOP_SOCKET_AUTH_TOKEN");
        String name = "";
        List<String> commandParts = new ArrayList<>();

        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--host" -> host = args[++i];
                    case "--port" -> port = Integer.parseInt(args[++i]);
                    case "--token" -> token = args[++i];
                    case "--timeout" -> timeout = Integer.parseInt(args[++i]);
                    case "--repeat" -> repeat = Integer.parseInt(args[++i]);
                    case "--interval" -> interval = Integer.parseInt(args[++i]);
                    case "--name" -> name = args[++i];
                    case "--help", "-h" -> {
                        printUsage();
                        return;
                    }
                    default -> commandParts.add(args[i]);
                }
            }
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            System.err.println("Sai cú pháp tham số.");
            printUsage();
            System.exit(2);
            return;
        }

        ShopSocketClient client = new ShopSocketClient(host, port, token, timeout, name);
        int exitCode = commandParts.isEmpty()
                ? client.runInteractive()
                : client.runOnce(String.join(" ", commandParts), repeat, interval);
        System.exit(exitCode);
    }

    // ===== Chế độ một lần =====

    private int runOnce(String command, int repeat, int intervalMillis) {
        try (Connection connection = connect()) {
            boolean failed = false;
            for (int i = 0; i < repeat; i++) {
                String response = connection.exchange(command);
                if (response == null || response.contains("\"status\":\"ERROR\"")) {
                    failed = true;
                }
                if (response == null) {
                    break;
                }
                if (i < repeat - 1) {
                    sleep(intervalMillis);
                }
            }
            connection.quitQuietly();
            return failed ? 1 : 0;
        } catch (IOException e) {
            reportConnectionError(e);
            return 1;
        }
    }

    // ===== Chế độ tương tác =====

    private int runInteractive() {
        try (Connection connection = connect();
             BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            out.println("Đã kết nối tới " + host + ":" + port + ". Gõ HELP để xem lệnh, exit để thoát.");
            while (true) {
                out.print("shop> ");
                out.flush();
                String line = keyboard.readLine();
                if (line == null || line.trim().equalsIgnoreCase("exit") || line.trim().equalsIgnoreCase("quit")) {
                    connection.quitQuietly();
                    return 0;
                }
                if (line.isBlank()) {
                    continue;
                }
                if (connection.exchange(line.trim()) == null) {
                    return 1;       // server đã đóng kết nối
                }
            }
        } catch (IOException e) {
            reportConnectionError(e);
            return 1;
        }
    }

    // ===== Kết nối =====

    private Connection connect() throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 5000);
        socket.setSoTimeout(timeoutMillis);
        Connection connection = new Connection(socket);
        if (token != null && !token.isBlank()) {
            String response = connection.exchange("AUTH " + token, "AUTH ***");
            if (response == null || response.contains("\"status\":\"ERROR\"")) {
                connection.close();
                throw new IOException("Xác thực thất bại.");
            }
        }
        return connection;
    }

    private void reportConnectionError(IOException e) {
        System.err.println("[" + now() + "] " + tag() + "Lỗi kết nối tới " + host + ":" + port + " - " + e.getMessage());
    }

    private final class Connection implements AutoCloseable {
        private final Socket socket;
        private final BufferedReader reader;
        private final OutputStream writer;

        Connection(Socket socket) throws IOException {
            this.socket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.writer = socket.getOutputStream();
        }

        String exchange(String command) throws IOException {
            return exchange(command, command);
        }

        /** Gửi một dòng và in phản hồi. Trả về null nếu server đã đóng kết nối hoặc quá thời gian chờ. */
        String exchange(String command, String shownCommand) throws IOException {
            long start = System.nanoTime();
            out.println("[" + now() + "] " + tag() + "> " + shownCommand);
            writer.write((command + "\n").getBytes(StandardCharsets.UTF_8));
            writer.flush();
            String response;
            try {
                response = reader.readLine();
            } catch (SocketTimeoutException e) {
                out.println("[" + now() + "] " + tag() + "! Quá " + timeoutMillis + " ms không có phản hồi.");
                return null;
            }
            long took = (System.nanoTime() - start) / 1_000_000;
            if (response == null) {
                out.println("[" + now() + "] " + tag() + "! Server đã đóng kết nối.");
                return null;
            }
            out.println("[" + now() + "] " + tag() + "< " + response + "   (" + took + " ms)");
            return response;
        }

        void quitQuietly() {
            try {
                writer.write("QUIT\n".getBytes(StandardCharsets.UTF_8));
                writer.flush();
            } catch (IOException ignored) {
                // server có thể đã đóng
            }
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }

    private String tag() {
        return label.isEmpty() ? "" : "(" + label + ") ";
    }

    private static String now() {
        return LocalTime.now().format(TIME);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void printUsage() {
        System.out.println("""
                Cách dùng:
                  java -jar shop-socket-client.jar [tùy chọn] [LỆNH ...]

                Không có LỆNH -> chế độ tương tác. Có LỆNH -> gửi lệnh đó rồi thoát.

                Lệnh của server : PING | CHECK_STOCK <sku> | GET_ORDER_STATUS <mã đơn> | HELP | QUIT

                Tùy chọn:
                  --host <ip>      (mặc định 127.0.0.1)
                  --port <cổng>    (mặc định 9099)
                  --token <token>  (hoặc biến môi trường SHOP_SOCKET_AUTH_TOKEN)
                  --timeout <ms>   (mặc định 10000)
                  --repeat <n>     gửi lệnh n lần (mặc định 1)
                  --interval <ms>  nghỉ giữa các lần (mặc định 1000)
                  --name <tên>     nhãn hiển thị để phân biệt nhiều client
                """);
    }
}
