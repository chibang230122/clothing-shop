const form = document.getElementById("invoice-email");
form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const button = form.querySelector("button");
    const status = form.querySelector("[role=status]");
    if (button.disabled) return;
    button.disabled = true;
    status.textContent = "Đang gửi hóa đơn…";
    try {
        const response = await fetch(form.action, {
            method: "POST", body: new FormData(form), headers: { Accept: "application/json" }
        });
        if (response.redirected || response.status === 403) {
            status.textContent = "Vui lòng đăng nhập lại bằng tài khoản có quyền gửi hóa đơn.";
            return;
        }
        const result = await response.json();
        status.textContent = result.message || "Không thể gửi hóa đơn cho đơn hàng này.";
    } catch {
        status.textContent = "Chưa xác nhận được kết quả gửi. Hãy kiểm tra hộp thư trước khi thử lại.";
    } finally {
        button.disabled = false;
    }
});
