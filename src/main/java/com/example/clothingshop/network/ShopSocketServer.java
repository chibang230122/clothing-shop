package com.example.clothingshop.network;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Socket Server của shop (TCP, dùng ServerSocket).
 *
 * Mô hình đa luồng: một luồng "acceptor" chỉ làm nhiệm vụ accept(); mỗi client được giao cho một
 * luồng trong ThreadPoolExecutor có giới hạn (maxClients). Khi pool đã đầy, kết nối mới nhận
 * lỗi BUSY rồi bị đóng, nhờ đó server không bị tạo luồng vô hạn.
 */
public final class ShopSocketServer {

    private final SocketServerSettings settings;
    private final CommandProcessor processor;
    private final Set<Socket> activeSockets = ConcurrentHashMap.newKeySet();

    private ServerSocket serverSocket;
    private ThreadPoolExecutor workers;
    private Thread acceptor;
    private volatile boolean running;

    public ShopSocketServer(SocketServerSettings settings, CommandProcessor processor) {
        this.settings = settings;
        this.processor = processor;
    }

    /** Mở cổng và bắt đầu nhận kết nối. Ném IOException nếu không bind được cổng. */
    public synchronized void start() throws IOException {
        if (running) {
            throw new IllegalStateException("Socket server đang chạy.");
        }

        ServerSocket ss = new ServerSocket();
        try {
            ss.setReuseAddress(true);
            ss.bind(new InetSocketAddress(InetAddress.getByName(settings.bindAddress()), settings.port()), 50);
        } catch (IOException | RuntimeException e) {
            ss.close();
            throw e;
        }
        serverSocket = ss;

        workers = new ThreadPoolExecutor(
                settings.maxClients(), settings.maxClients(),
                0L, TimeUnit.SECONDS,
                new SynchronousQueue<>(),                 // không xếp hàng chờ: hết chỗ thì báo BUSY
                new NamedThreadFactory("shop-socket-worker-"),
                new ThreadPoolExecutor.AbortPolicy());

        running = true;
        acceptor = new Thread(this::acceptLoop, "shop-socket-acceptor");
        acceptor.setDaemon(true);
        acceptor.start();

        SocketLog.info("Socket server đang chạy tại %s:%d (tối đa %d client, timeout %d ms, dòng tối đa %d byte%s)",
                settings.bindAddress(), getPort(), settings.maxClients(), settings.idleTimeoutMillis(),
                settings.maxLineBytes(), processor.isAuthRequired() ? ", yêu cầu AUTH" : "");
    }

    /** Dừng nhận kết nối mới, đóng các kết nối đang mở và giải phóng thread pool. */
    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;

        closeQuietly(serverSocket);                      // làm accept() thoát
        workers.shutdown();
        for (Socket s : activeSockets) {                 // làm các luồng đang chờ đọc thoát
            closeQuietly(s);
        }
        try {
            if (!workers.awaitTermination(5, TimeUnit.SECONDS)) {
                workers.shutdownNow();
                workers.awaitTermination(2, TimeUnit.SECONDS);
            }
            acceptor.join(2000);
        } catch (InterruptedException e) {
            workers.shutdownNow();
            Thread.currentThread().interrupt();
        }
        activeSockets.clear();
        SocketLog.info("Socket server đã dừng.");
    }

    public boolean isRunning() {
        return running;
    }

    /** Cổng thực tế đang lắng nghe (hữu ích khi cấu hình port = 0). */
    public int getPort() {
        return serverSocket == null ? -1 : serverSocket.getLocalPort();
    }

    /** Số client đang được phục vụ. */
    public int getActiveClients() {
        return activeSockets.size();
    }

    // ===== Nội bộ =====

    private void acceptLoop() {
        while (running) {
            Socket client;
            try {
                client = serverSocket.accept();
            } catch (IOException e) {
                if (!running) {
                    break;                               // server đang tắt
                }
                SocketLog.warn("accept() lỗi: %s", e.getMessage());
                pause(100);                              // tránh vòng lặp nóng khi lỗi lặp lại
                continue;
            }
            dispatch(client);
        }
    }

    private void dispatch(Socket client) {
        activeSockets.add(client);
        try {
            workers.execute(new ClientHandler(client, processor, settings, () -> activeSockets.remove(client)));
        } catch (RejectedExecutionException e) {
            activeSockets.remove(client);
            rejectBusy(client);
        }
    }

    private void rejectBusy(Socket client) {
        SocketLog.warn("Từ chối %s: đã đủ %d client (BUSY)", client.getRemoteSocketAddress(), settings.maxClients());
        try (Socket s = client) {
            ProtocolReply busy = CommandProcessor.error("UNKNOWN", "BUSY",
                    "Server đang phục vụ tối đa số client cho phép, vui lòng thử lại sau.", true);
            OutputStream out = s.getOutputStream();
            out.write((busy.json() + "\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException ignored) {
            // client đã đi mất, không cần làm gì thêm
        }
    }

    private static void closeQuietly(java.io.Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException ignored) {
            // đóng "im lặng"
        }
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger counter = new AtomicInteger(1);

        NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread t = new Thread(runnable, prefix + counter.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }
}
