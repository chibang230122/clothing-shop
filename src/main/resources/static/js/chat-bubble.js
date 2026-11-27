document.addEventListener("DOMContentLoaded", () => {
    const toggleBtn = document.getElementById("chat-bubble-toggle");
    const popupBox = document.getElementById("chat-popup-box");
    const closeBtn = document.getElementById("chat-popup-close");
    const unreadDot = document.getElementById("chat-unread-dot");
    const agentInfoEl = document.getElementById("chat-agent-info");
    const claimStatusEl = document.getElementById("chat-claim-status");
    const messagesBox = document.getElementById("chat-popup-messages");
    const noticeBox = document.getElementById("chat-popup-notice");
    const form = document.getElementById("chat-popup-form");
    const input = document.getElementById("chat-popup-input");

    if (!toggleBtn || !popupBox) return;

    let conversationId = null;
    let isClaimed = false;
    let currentAgentName = "";
    const bridge = new ShopChatBridgeClient();

    function shortDisplayName(rawName) {
        if (!rawName) return "Nhân viên";
        const s = String(rawName).trim();
        if (s.includes("@")) {
            return s.split("@")[0];
        }
        return s;
    }

    function formatTime(isoOrDate) {
        if (!isoOrDate) {
            return new Date().toTimeString().slice(0, 5);
        }
        const d = new Date(isoOrDate);
        if (!Number.isNaN(d.getTime())) {
            return d.toTimeString().slice(0, 5);
        }
        const s = String(isoOrDate);
        const tIdx = s.indexOf("T");
        return tIdx >= 0 ? s.slice(tIdx + 1, tIdx + 6) : s.slice(0, 5);
    }

    function renderConversationState(claimed, agentName, statusText) {
        isClaimed = Boolean(claimed);
        if (agentName && String(agentName).trim() !== "") {
            currentAgentName = shortDisplayName(agentName);
        }
        if (agentInfoEl) {
            agentInfoEl.textContent = isClaimed
                ? `Hỗ trợ: ${currentAgentName || "Nhân viên"}`
                : "Hỗ trợ: Chờ tiếp nhận";
        }
        if (claimStatusEl) {
            const label = statusText || (isClaimed ? "Đã tiếp nhận" : "Chưa tiếp nhận");
            claimStatusEl.textContent = label;
            if (isClaimed) {
                claimStatusEl.style.color = "#15803d";
                claimStatusEl.style.background = "#dcfce7";
            } else {
                claimStatusEl.style.color = "#b45309";
                claimStatusEl.style.background = "#fef3c7";
            }
        }
    }

    function appendMessage(text, isMe, senderLabel, timeStr, snapshotStatus) {
        const wrapper = document.createElement("div");
        wrapper.style.display = "flex";
        wrapper.style.flexDirection = "column";
        wrapper.style.alignItems = isMe ? "flex-end" : "flex-start";
        wrapper.style.marginBottom = "8px";
        wrapper.style.maxWidth = "100%";

        if (!isMe && senderLabel) {
            const nameTag = document.createElement("div");
            nameTag.style.fontSize = "11px";
            nameTag.style.color = "#64748b";
            nameTag.style.margin = "0 4px 2px 4px";
            nameTag.textContent = shortDisplayName(senderLabel);
            wrapper.appendChild(nameTag);
        }

        const bubble = document.createElement("div");
        bubble.className = "chat-msg-item " + (isMe ? "chat-msg-me" : "chat-msg-other");
        bubble.style.textAlign = "left";
        bubble.style.maxWidth = "78%";
        bubble.style.padding = "7px 11px";
        bubble.style.borderRadius = "14px";
        bubble.style.wordBreak = "break-word";

        const body = document.createElement("div");
        body.style.fontSize = "13.5px";
        body.style.lineHeight = "1.35";
        body.textContent = text;

        const footer = document.createElement("div");
        footer.style.fontSize = "10px";
        footer.style.opacity = "0.72";
        footer.style.marginTop = "3px";
        footer.style.textAlign = "right";
        footer.style.whiteSpace = "nowrap";
        footer.textContent = (isMe && snapshotStatus)
            ? `${timeStr} • ${snapshotStatus}`
            : timeStr;

        bubble.appendChild(body);
        bubble.appendChild(footer);
        wrapper.appendChild(bubble);

        messagesBox.appendChild(wrapper);
        messagesBox.scrollTop = messagesBox.scrollHeight;
    }

    function showNotice(msg) {
        noticeBox.textContent = msg;
        noticeBox.classList.remove("hidden");
        setTimeout(() => noticeBox.classList.add("hidden"), 4000);
    }

    bridge.onFrame = (frame) => {
        if (!frame || !frame.payload) return;
        if (frame.type === "CONVERSATION_CLAIMED") {
            const cid = Number(frame.payload.conversationId);
            if (!conversationId || cid === Number(conversationId)) {
                const staffLabel = frame.payload.assignedStaffName || `Staff #${frame.payload.assignedStaffId || ""}`;
                renderConversationState(true, staffLabel, "Đã tiếp nhận");
            }
            return;
        }
        if (frame.type === "CHAT_MESSAGE") {
            const p = frame.payload;
            if (conversationId && p.conversationId && Number(p.conversationId) !== Number(conversationId)) {
                return;
            }
            const isCustomer = p.senderRole === "CUSTOMER";
            const senderLabel = isCustomer
                ? "Bạn"
                : (p.senderName || currentAgentName || "Nhân viên");
            const timeStr = formatTime(p.createdAt);
            const snapStatus = isCustomer ? (isClaimed ? "Đã tiếp nhận" : "Chưa tiếp nhận") : "";
            if (!isCustomer && p.senderName && !isClaimed) {
                renderConversationState(true, p.senderName, "Đã tiếp nhận");
            }
            appendMessage(p.content, isCustomer, senderLabel, timeStr, snapStatus);
            if (p.masked) {
                showNotice("Tin có từ không phù hợp đã bị che.");
            }
            if (popupBox.classList.contains("hidden")) {
                unreadDot.classList.remove("hidden");
            }
        }
    };

    bridge.onDisconnect = () => {
        if (!popupBox.classList.contains("hidden")) {
            setTimeout(() => {
                if (!popupBox.classList.contains("hidden") && !bridge.isConnected()) {
                    initAndLoadChat().catch(() => {});
                }
            }, 1500);
        }
    };

    async function initAndLoadChat() {
        await bridge.connect();
        const res = await bridge.openChat();
        if (res.ok && res.payload) {
            const conv = res.payload.conversation || {};
            conversationId = conv.id;
            const claimed = conv.assignedStaffId != null;
            const staffName = res.payload.assignedStaffName
                || (conv.assignedStaffId != null ? `Staff #${conv.assignedStaffId}` : "");
            renderConversationState(claimed, staffName, claimed ? "Đã tiếp nhận" : "Chưa tiếp nhận");

            messagesBox.innerHTML = "";
            (res.payload.messages || []).forEach(m => {
                const isMe = Number(m.senderId) === Number(conv.customerId);
                const who = isMe
                    ? "Bạn"
                    : (m.senderName || currentAgentName || `Nhân viên #${m.senderId}`);
                const ts = formatTime(m.createdAt);
                const st = isMe ? (claimed ? "Đã tiếp nhận" : "Chưa tiếp nhận") : "";
                appendMessage(m.filteredContent, isMe, who, ts, st);
            });
        }
    }

    toggleBtn.addEventListener("click", async () => {
        popupBox.classList.toggle("hidden");
        if (!popupBox.classList.contains("hidden")) {
            unreadDot.classList.add("hidden");
            if (!bridge.isConnected() || !conversationId) {
                await initAndLoadChat();
            }
        }
    });

    closeBtn.addEventListener("click", () => popupBox.classList.add("hidden"));

    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        const text = input.value.trim();
        if (!text) return;
        input.value = "";
        try {
            if (!bridge.isConnected() || !conversationId) {
                await initAndLoadChat();
            }
            if (!conversationId) {
                showNotice("Không thể kết nối phòng hỗ trợ, vui lòng thử lại.");
                return;
            }
            const res = await bridge.sendChat(conversationId, text);
            if (!res.ok) {
                showNotice(res.error || "Tin nhắn bị chặn bởi bộ lọc.");
            }
        } catch (err) {
            showNotice("Mất kết nối máy chủ, đang kết nối lại...");
            await initAndLoadChat().catch(() => {});
        }
    });
});