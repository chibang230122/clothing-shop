package com.clothingshop.socket.v2;

import com.clothingshop.socket.BoundedLineReader;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ShopChatSocketServer implements SmartLifecycle {

    private final ChatSocketProperties props;
    private final V2CommandDispatcher dispatcher;
    private final RealtimeSessionRegistry registry;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private ServerSocket serverSocket;
    private ThreadPoolExecutor workerPool;
    private Thread acceptorThread;

    public ShopChatSocketServer(ChatSocketProperties props,
                                V2CommandDispatcher dispatcher,
                                RealtimeSessionRegistry registry) {
        this.props = props;
        this.dispatcher = dispatcher;
        this.registry = registry;
    }

    @Override
    public void start() {
        if (!props.isEnabled() || running.get()) return;
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(props.getHost(), props.getPort()));

            AtomicInteger threadSeq = new AtomicInteger(1);
            workerPool = new ThreadPoolExecutor(
                props.getCoreThreads(),
                props.getMaxThreads(),
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                r -> {
                    Thread t = new Thread(r, "chat-v2-worker-" + threadSeq.getAndIncrement());
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.AbortPolicy()
            );

            running.set(true);
            acceptorThread = new Thread(this::acceptLoop, "chat-v2-acceptor-" + props.getPort());
            acceptorThread.setDaemon(true);
            acceptorThread.start();
        } catch (BindException e) {
            System.err.println("[WARN] Cong Chat Socket " + props.getPort() + " dang duoc su dung, bo qua khoi dong trung lap.");
        } catch (IOException e) {
            throw new IllegalStateException("Khong the khoi dong Chat Socket Server tren cong " + props.getPort(), e);
        }
    }

    private void acceptLoop() {
        while (running.get() && !serverSocket.isClosed()) {
            try {
                Socket client = serverSocket.accept();
                if (registry.activeCount() >= props.getMaxClients()) {
                    rejectBusy(client);
                    continue;
                }
                workerPool.execute(() -> handleClient(client));
            } catch (RejectedExecutionException ex) {
                // Pool full
            } catch (IOException ex) {
                if (!running.get()) break;
            }
        }
    }

    private void rejectBusy(Socket client) {
        try (client) {
            client.getOutputStream().write(
                "{\"v\":2,\"type\":\"ERROR\",\"ok\":false,\"errorCode\":\"BUSY\",\"error\":\"Chat server full\"}\n"
                    .getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
        }
    }

    private void handleClient(Socket socket) {
        ChatClientSession session = null;
        try (socket) {
            socket.setSoTimeout(props.getIdleTimeoutSeconds() * 1000);
            session = new ChatClientSession(socket);
            registry.register(session);

            session.sendLine("{\"v\":2,\"type\":\"WELCOME\",\"ok\":true,\"payload\":{\"protocol\":\"JSON-LINE-V2\",\"port\":"
                + props.getPort() + ",\"handledBy\":\"" + Thread.currentThread().getName() + "\"}}");

            BoundedLineReader reader = new BoundedLineReader(socket.getInputStream(), props.getMaxLineBytes());
            while (running.get() && !socket.isClosed()) {
                String line;
                try {
                    line = reader.readLine();
                } catch (BoundedLineReader.LineTooLongException ex) {
                    session.sendLine("{\"v\":2,\"type\":\"ERROR\",\"ok\":false,\"errorCode\":\"LINE_TOO_LONG\",\"error\":\"Gói tin vượt quá giới hạn\"}");
                    break;
                }
                if (line == null) break;
                if (line.isBlank()) continue;

                String response = dispatcher.dispatch(line, session);
                session.sendLine(response);
            }
        } catch (SocketTimeoutException ignored) {
        } catch (Exception ignored) {
        } finally {
            if (session != null) {
                registry.unregister(session);
            }
        }
    }

    @Override
    public void stop() {
        running.set(false);
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
        if (workerPool != null) {
            workerPool.shutdownNow();
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    public int getBoundPort() {
        return serverSocket != null ? serverSocket.getLocalPort() : props.getPort();
    }
}
