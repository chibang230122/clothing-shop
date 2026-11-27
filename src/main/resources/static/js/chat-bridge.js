class ShopChatBridgeClient {
    constructor(wsUrl = `${window.location.protocol === 'https:' ? 'wss' : 'ws'}://${window.location.host}/ws/tcp-bridge`) {
        this.wsUrl = wsUrl;
        this.ws = null;
        this.seq = 1;
        this.listeners = new Map();
        this.onFrame = null;
        this.onDisconnect = null;
        this.pingTimer = null;
        this.connectingPromise = null;
    }

    isConnected() {
        return this.ws && this.ws.readyState === WebSocket.OPEN;
    }

    connect() {
        if (this.isConnected()) {
            return Promise.resolve();
        }
        if (this.connectingPromise) {
            return this.connectingPromise;
        }
        this.connectingPromise = new Promise((resolve, reject) => {
            const socket = new WebSocket(this.wsUrl);
            this.ws = socket;

            socket.onopen = () => {
                this.connectingPromise = null;
                this.startHeartbeat();
                resolve();
            };

            socket.onerror = (err) => {
                this.connectingPromise = null;
                reject(err);
            };

            socket.onclose = () => {
                this.connectingPromise = null;
                this.stopHeartbeat();
                for (const [reqId, cb] of this.listeners.entries()) {
                    cb({ ok: false, error: "Kết nối bị gián đoạn, đang thử kết nối lại..." });
                    this.listeners.delete(reqId);
                }
                if (this.onDisconnect) this.onDisconnect();
            };

            socket.onmessage = (evt) => {
                const frame = JSON.parse(evt.data);
                if (this.onFrame) this.onFrame(frame);
                if (frame.requestId && this.listeners.has(frame.requestId)) {
                    const cb = this.listeners.get(frame.requestId);
                    this.listeners.delete(frame.requestId);
                    cb(frame);
                }
            };
        });
        return this.connectingPromise;
    }

    startHeartbeat() {
        this.stopHeartbeat();
        this.pingTimer = setInterval(() => {
            if (this.isConnected()) {
                this.ws.send(JSON.stringify({ v: 2, type: "PING", requestId: `ping-${Date.now()}`, payload: {} }));
            }
        }, 25000);
    }

    stopHeartbeat() {
        if (this.pingTimer) {
            clearInterval(this.pingTimer);
            this.pingTimer = null;
        }
    }

    async sendCommand(type, payload = {}) {
        if (!this.isConnected()) {
            await this.connect();
        }
        const requestId = `web-${this.seq++}`;
        return new Promise((resolve) => {
            const timer = setTimeout(() => {
                if (this.listeners.has(requestId)) {
                    this.listeners.delete(requestId);
                    resolve({ ok: false, error: "Hết thời gian chờ phản hồi từ máy chủ." });
                }
            }, 8000);

            this.listeners.set(requestId, (frame) => {
                clearTimeout(timer);
                resolve(frame);
            });
            this.ws.send(JSON.stringify({ v: 2, type, requestId, payload }));
        });
    }

    login(username, password) {
        return this.sendCommand("AUTH", { username, password });
    }

    openChat() {
        return this.sendCommand("OPEN_CHAT");
    }

    sendChat(conversationId, content) {
        return this.sendCommand("SEND_CHAT", { conversationId, content });
    }
}
window.ShopChatBridgeClient = ShopChatBridgeClient;