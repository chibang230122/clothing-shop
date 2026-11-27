package com.clothingshop.client.lib;

import com.clothingshop.client.swing.MiniJson;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ShopSocketConnection implements Closeable {

    public enum LogLevel { OUTGOING, INCOMING, ERROR, INFO }

    public interface LogListener {
        void onLog(LogLevel level, String message, Long latencyMs);
    }

    public record ResponseEnvelope(Map<String, Object> data, long latencyMs) {}

    private static final int CONNECT_TIMEOUT_MS = 5000;

    private Socket socket;
    private BufferedWriter writer;
    private Thread readerThread;
    private volatile boolean connected = false;

    private final AtomicInteger seq = new AtomicInteger(1);
    private final Map<String, CompletableFuture<ResponseEnvelope>> pendingRequests = new ConcurrentHashMap<>();
    private final Map<String, Long> requestStartNanos = new ConcurrentHashMap<>();

    private Consumer<Map<String, Object>> pushListener;
    private LogListener logListener;
    private Consumer<String> disconnectListener;

    public void setPushListener(Consumer<Map<String, Object>> listener) { this.pushListener = listener; }
    public void setLogListener(LogListener listener) { this.logListener = listener; }
    public void setDisconnectListener(Consumer<String> listener) { this.disconnectListener = listener; }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    public synchronized void connect(String host, int port) throws IOException {
        close();
        emitLog(LogLevel.INFO, "Đang kết nối tới Socket Server " + host + ":" + port + "...", null);
        Socket s = new Socket();
        s.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);

        this.socket = s;
        this.writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8));
        BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
        this.connected = true;

        readerThread = new Thread(() -> readLoop(reader), "shop-client-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void readLoop(BufferedReader reader) {
        String disconnectReason = "Server đã đóng kết nối";
        try {
            String line;
            while (connected && (line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                    Map<String, Object> frame = MiniJson.parseObject(trimmed);
                    Object reqIdObj = frame.get("requestId");
                    if (reqIdObj != null) {
                        String reqId = String.valueOf(reqIdObj);
                        Long startNs = requestStartNanos.remove(reqId);
                        long latencyMs = startNs != null ? TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs) : 0L;
                        emitLog(LogLevel.INCOMING, "< " + maskSensitive(trimmed), latencyMs);

                        CompletableFuture<ResponseEnvelope> future = pendingRequests.remove(reqId);
                        if (future != null) {
                            future.complete(new ResponseEnvelope(frame, latencyMs));
                        }
                    } else {
                        emitLog(LogLevel.INCOMING, "< [PUSH] " + maskSensitive(trimmed), null);
                        if (pushListener != null) {
                            pushListener.accept(frame);
                        }
                    }
                } else {
                    emitLog(LogLevel.INCOMING, "< " + maskSensitive(trimmed), null);
                }
            }
        } catch (IOException ex) {
            if (connected) {
                disconnectReason = ex.getMessage();
                emitLog(LogLevel.ERROR, "[LỖI KẾT NỐI] " + disconnectReason, null);
            }
        } finally {
            connected = false;
            pendingRequests.forEach((id, f) -> f.completeExceptionally(new IOException("Mất kết nối tới server")));
            pendingRequests.clear();
            requestStartNanos.clear();
            if (disconnectListener != null) {
                disconnectListener.accept(disconnectReason);
            }
        }
    }

    public ResponseEnvelope sendJsonCommand(String type, Map<String, Object> payload, long timeoutSeconds) throws Exception {
        if (!isConnected()) {
            throw new IOException("Chưa kết nối tới Socket Server");
        }
        String reqId = "req-" + seq.getAndIncrement();
        Map<String, Object> packet = new LinkedHashMap<>();
        packet.put("v", 2);
        packet.put("type", type);
        packet.put("requestId", reqId);
        packet.put("payload", payload != null ? payload : Map.of());

        String rawJson = MiniJson.stringify(packet);
        CompletableFuture<ResponseEnvelope> future = new CompletableFuture<>();
        pendingRequests.put(reqId, future);
        requestStartNanos.put(reqId, System.nanoTime());

        synchronized (this) {
            writer.write(rawJson);
            writer.newLine();
            writer.flush();
        }
        emitLog(LogLevel.OUTGOING, "> " + maskSensitive(rawJson), null);

        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException te) {
            pendingRequests.remove(reqId);
            requestStartNanos.remove(reqId);
            emitLog(LogLevel.ERROR, "[LỖI TIMEOUT] Lệnh " + type + " quá thời gian chờ (" + timeoutSeconds + "s)", null);
            throw te;
        }
    }

    public static String maskSensitive(String text) {
        if (text == null) return "";
        String masked = text.replaceAll("(\"(?:password|token|secret)\"\\s*:\\s*\")[^\"]*(\")", "$1***$2");
        masked = masked.replaceAll("(?i)(AUTH\\s+)\\S+", "$1***");
        return masked;
    }

    private void emitLog(LogLevel level, String msg, Long latencyMs) {
        if (logListener != null) {
            logListener.onLog(level, msg, latencyMs);
        }
    }

    @Override
    public synchronized void close() {
        connected = false;
        if (socket != null) {
            try { socket.close(); } catch (IOException ignored) {}
            socket = null;
        }
    }
}
