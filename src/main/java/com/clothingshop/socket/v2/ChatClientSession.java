package com.clothingshop.socket.v2;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class ChatClientSession {

    private final String sessionId = UUID.randomUUID().toString();
    private final Socket socket;
    private final BufferedWriter writer;

    private volatile Integer userId;
    private volatile String username;
    private volatile String role;

    public ChatClientSession(Socket socket) throws IOException {
        this.socket = socket;
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    public synchronized void sendLine(String jsonLine) {
        try {
            if (!socket.isClosed()) {
                writer.write(jsonLine);
                writer.newLine();
                writer.flush();
            }
        } catch (IOException ignored) {
        }
    }

    public boolean isAuthenticated() {
        return userId != null && role != null;
    }

    public void authenticate(Integer userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    public String getSessionId() { return sessionId; }
    public Integer getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public Socket getSocket() { return socket; }
}
