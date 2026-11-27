package com.clothingshop.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final TcpBridgeWebSocketHandler bridgeHandler;

    public WebSocketConfig(TcpBridgeWebSocketHandler bridgeHandler) {
        this.bridgeHandler = bridgeHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(bridgeHandler, "/ws/tcp-bridge")
                .setAllowedOriginPatterns("*");
    }
}
