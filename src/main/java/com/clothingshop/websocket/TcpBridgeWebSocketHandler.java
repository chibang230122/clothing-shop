package com.clothingshop.websocket;

import com.clothingshop.socket.v2.ChatSocketProperties;
import com.clothingshop.socket.v2.V2CommandDispatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TcpBridgeWebSocketHandler extends TextWebSocketHandler {

    private record BridgeLink(Socket tcpSocket, BufferedWriter tcpWriter) {}

    private final ChatSocketProperties props;
    private final Map<String, BridgeLink> links = new ConcurrentHashMap<>();

    public TcpBridgeWebSocketHandler(ChatSocketProperties props) {
        this.props = props;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession wsSession) throws Exception {
        openTcpLink(wsSession);
    }

    private BridgeLink openTcpLink(WebSocketSession wsSession) throws IOException {
        closeLinkOnly(wsSession.getId());
        Socket tcpSocket = new Socket("127.0.0.1", props.getPort());
        BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(tcpSocket.getOutputStream(), StandardCharsets.UTF_8));
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(tcpSocket.getInputStream(), StandardCharsets.UTF_8));

        BridgeLink link = new BridgeLink(tcpSocket, writer);
        links.put(wsSession.getId(), link);

        Principal principal = wsSession.getPrincipal();
        if (principal != null && principal.getName() != null) {
            String safeUser = principal.getName().replace("\\", "\\\\").replace("\"", "\\\"");
            String authCmd = "{\"v\":2,\"type\":\"AUTH\",\"requestId\":\"bridge-init\",\"payload\":{\"username\":\""
                    + safeUser + "\",\"bridgeSecret\":\"" + V2CommandDispatcher.INTERNAL_BRIDGE_SECRET + "\"}}";
            synchronized (writer) {
                writer.write(authCmd);
                writer.newLine();
                writer.flush();
            }
        }

        Thread pumpThread = new Thread(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null && wsSession.isOpen()) {
                    if (line.contains("\"requestId\":\"bridge-init\"")) {
                        continue;
                    }
                    synchronized (wsSession) {
                        if (wsSession.isOpen()) {
                            wsSession.sendMessage(new TextMessage(line));
                        }
                    }
                }
            } catch (IOException ignored) {
            } finally {
                closeLinkOnly(wsSession.getId());
                try {
                    if (wsSession.isOpen()) {
                        wsSession.close(CloseStatus.NORMAL);
                    }
                } catch (IOException ignored) {
                }
            }
        }, "ws-tcp-bridge-" + wsSession.getId());
        pumpThread.setDaemon(true);
        pumpThread.start();
        return link;
    }

    @Override
    protected void handleTextMessage(WebSocketSession wsSession, TextMessage message) throws Exception {
        BridgeLink link = links.get(wsSession.getId());
        if (link == null || link.tcpSocket().isClosed()) {
            link = openTcpLink(wsSession);
        }
        String cleanLine = message.getPayload().replace("\r", "").replace("\n", " ");
        synchronized (link.tcpWriter()) {
            link.tcpWriter().write(cleanLine);
            link.tcpWriter().newLine();
            link.tcpWriter().flush();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession wsSession, CloseStatus status) {
        closeLinkOnly(wsSession.getId());
    }

    private void closeLinkOnly(String wsSessionId) {
        BridgeLink link = links.remove(wsSessionId);
        if (link != null) {
            try {
                link.tcpSocket().close();
            } catch (IOException ignored) {
            }
        }
    }
}