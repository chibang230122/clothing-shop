package com.example.clothingshop.network;

import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Nối Socket Server với vòng đời của ứng dụng Spring: tự khởi động khi website khởi động
 * và tự đóng (socket, stream, thread pool) khi website tắt.
 *
 * Nếu không mở được cổng (ví dụ cổng đang bị chương trình khác dùng), chỉ ghi log lỗi; website vẫn chạy bình thường.
 */
@Component
public class ShopSocketServerLifecycle implements SmartLifecycle {

    private final ShopSocketProperties properties;
    private final StockLookup stockLookup;
    private final OrderStatusLookup orderStatusLookup;

    private volatile ShopSocketServer server;

    public ShopSocketServerLifecycle(ShopSocketProperties properties,
                                     StockLookup stockLookup,
                                     OrderStatusLookup orderStatusLookup) {
        this.properties = properties;
        this.stockLookup = stockLookup;
        this.orderStatusLookup = orderStatusLookup;
    }

    @Override
    public void start() {
        if (!properties.isEnabled()) {
            SocketLog.info("Socket server đang tắt (shop.socket.enabled=false).");
            return;
        }
        try {
            String token = properties.getAuthToken();
            boolean openToNetwork = !"127.0.0.1".equals(properties.getBindAddress())
                    && !"localhost".equalsIgnoreCase(properties.getBindAddress());
            if (openToNetwork && (token == null || token.isBlank())) {
                SocketLog.warn("Socket server mở cho mạng ngoài (%s) nhưng chưa đặt SHOP_SOCKET_AUTH_TOKEN.",
                        properties.getBindAddress());
            }

            ShopSocketServer candidate = new ShopSocketServer(
                    properties.toSettings(),
                    new CommandProcessor(stockLookup, orderStatusLookup, token));
            candidate.start();
            server = candidate;
        } catch (IOException | RuntimeException e) {
            SocketLog.error("Không thể khởi động Socket server (cổng " + properties.getPort()
                    + "). Website vẫn chạy bình thường.", e);
        }
    }

    @Override
    public void stop() {
        ShopSocketServer current = server;
        server = null;
        if (current != null) {
            current.stop();
        }
    }

    @Override
    public boolean isRunning() {
        ShopSocketServer current = server;
        return current != null && current.isRunning();
    }

    /** Cổng thực tế đang lắng nghe, hoặc -1 nếu server không chạy. */
    public int getPort() {
        ShopSocketServer current = server;
        return current == null ? -1 : current.getPort();
    }

    public int getActiveClients() {
        ShopSocketServer current = server;
        return current == null ? 0 : current.getActiveClients();
    }
}
