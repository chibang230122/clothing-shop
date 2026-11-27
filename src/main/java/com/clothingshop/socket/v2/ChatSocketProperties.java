package com.clothingshop.socket.v2;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "shop.chat-socket")
public class ChatSocketProperties {
    private boolean enabled = true;
    private String host = "0.0.0.0";
    private int port = 9100;
    private int maxClients = 60;
    private int coreThreads = 8;
    private int maxThreads = 60;
    private int idleTimeoutSeconds = 300;
    private int maxLineBytes = 8192;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public int getMaxClients() { return maxClients; }
    public void setMaxClients(int maxClients) { this.maxClients = maxClients; }
    public int getCoreThreads() { return coreThreads; }
    public void setCoreThreads(int coreThreads) { this.coreThreads = coreThreads; }
    public int getMaxThreads() { return maxThreads; }
    public void setMaxThreads(int maxThreads) { this.maxThreads = maxThreads; }
    public int getIdleTimeoutSeconds() { return idleTimeoutSeconds; }
    public void setIdleTimeoutSeconds(int idleTimeoutSeconds) { this.idleTimeoutSeconds = idleTimeoutSeconds; }
    public int getMaxLineBytes() { return maxLineBytes; }
    public void setMaxLineBytes(int maxLineBytes) { this.maxLineBytes = maxLineBytes; }
}
