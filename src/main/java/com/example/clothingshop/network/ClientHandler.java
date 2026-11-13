package com.example.clothingshop.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/**
 * Phục vụ MỘT client trên MỘT luồng của thread pool: đọc từng dòng request, xử lý, trả lời.
 * Socket và stream luôn được đóng trong khối finally/try-with-resources, kể cả khi client
 * ngắt kết nối đột ngột hoặc quá thời gian chờ.
 */
final class ClientHandler implements Runnable {

    private final Socket socket;
    private final CommandProcessor processor;
    private final SocketServerSettings settings;
    private final Runnable onFinished;

    ClientHandler(Socket socket, CommandProcessor processor, SocketServerSettings settings, Runnable onFinished) {
        this.socket = socket;
        this.processor = processor;
        this.settings = settings;
        this.onFinished = onFinished;
    }

    @Override
    public void run() {
        String thread = Thread.currentThread().getName();
        String client = String.valueOf(socket.getRemoteSocketAddress());
        int requestCount = 0;
        SocketLog.info("thread=%s client=%s CONNECTED", thread, client);

        try (Socket s = socket;
             InputStream in = s.getInputStream();
             OutputStream out = s.getOutputStream()) {

            s.setSoTimeout(settings.idleTimeoutMillis());
            s.setTcpNoDelay(true);

            BoundedLineReader reader = new BoundedLineReader(in, settings.maxLineBytes());
            CommandProcessor.Session session = processor.newSession();

            while (!Thread.currentThread().isInterrupted()) {
                String line;
                try {
                    line = reader.readLine();
                } catch (LineTooLongException e) {
                    send(out, CommandProcessor.error("UNKNOWN", "LINE_TOO_LONG",
                            "Dòng request dài quá " + settings.maxLineBytes() + " byte.", true));
                    SocketLog.warn("thread=%s client=%s LINE_TOO_LONG -> đóng kết nối", thread, client);
                    break;
                } catch (SocketTimeoutException e) {
                    send(out, CommandProcessor.error("UNKNOWN", "TIMEOUT",
                            "Quá " + settings.idleTimeoutMillis() + " ms không nhận được yêu cầu.", true));
                    SocketLog.warn("thread=%s client=%s TIMEOUT -> đóng kết nối", thread, client);
                    break;
                }

                if (line == null) {
                    SocketLog.info("thread=%s client=%s client đã đóng kết nối", thread, client);
                    break;
                }

                long startNanos = System.nanoTime();
                ProtocolReply reply = processor.process(line, session);
                send(out, reply);
                requestCount++;

                long tookMs = (System.nanoTime() - startNanos) / 1_000_000;
                SocketLog.info("thread=%s client=%s request=\"%s\" status=%s%s took=%dms",
                        thread, client, describeRequest(line), reply.status(),
                        reply.errorCode() == null ? "" : "(" + reply.errorCode() + ")", tookMs);

                if (reply.closeConnection()) {
                    break;
                }
            }
        } catch (IOException e) {
            // Client ngắt đột ngột (reset), hoặc server đang tắt và đóng socket.
            SocketLog.warn("thread=%s client=%s mất kết nối: %s", thread, client, e.getMessage());
        } catch (RuntimeException e) {
            SocketLog.error("thread=" + thread + " client=" + client + " lỗi không mong đợi", e);
        } finally {
            onFinished.run();
            SocketLog.info("thread=%s client=%s CLOSED (đã xử lý %d yêu cầu)", thread, client, requestCount);
        }
    }

    private static void send(OutputStream out, ProtocolReply reply) throws IOException {
        out.write((reply.json() + "\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    /** Không ghi token vào log. */
    private static String describeRequest(String line) {
        String trimmed = line.trim();
        if (trimmed.regionMatches(true, 0, "AUTH", 0, 4)) {
            return "AUTH ***";
        }
        return SocketLog.clean(trimmed, 100);
    }
}
