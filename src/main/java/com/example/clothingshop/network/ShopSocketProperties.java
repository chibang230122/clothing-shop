package com.example.clothingshop.network;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cấu hình Socket Server, đọc từ application.properties (tiền tố shop.socket.*).
 * Token xác thực nên đặt qua biến môi trường SHOP_SOCKET_AUTH_TOKEN, không ghi vào file commit lên Git.
 */
@Component
@ConfigurationProperties(prefix = "shop.socket")
public class ShopSocketProperties {

    private boolean enabled = true;
    private int port = 9099;
    private String bindAddress = "127.0.0.1";
    private int maxClients = 20;
    private int idleTimeoutMs = 60_000;
    private int maxLineLength = 512;
    private String authToken = "";

    public SocketServerSettings toSettings() {
        return new SocketServerSettings(port, bindAddress, maxClients, idleTimeoutMs, maxLineLength);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getBindAddress() {
        return bindAddress;
    }

    public void setBindAddress(String bindAddress) {
        this.bindAddress = bindAddress;
    }

    public int getMaxClients() {
        return maxClients;
    }

    public void setMaxClients(int maxClients) {
        this.maxClients = maxClients;
    }

    public int getIdleTimeoutMs() {
        return idleTimeoutMs;
    }

    public void setIdleTimeoutMs(int idleTimeoutMs) {
        this.idleTimeoutMs = idleTimeoutMs;
    }

    public int getMaxLineLength() {
        return maxLineLength;
    }

    public void setMaxLineLength(int maxLineLength) {
        this.maxLineLength = maxLineLength;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }
}
