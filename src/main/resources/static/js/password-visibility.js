document.addEventListener("click", (event) => {
    const button = event.target.closest("[data-password-toggle]");
    if (!button) {
        return;
    }

    const input = document.getElementById(button.getAttribute("aria-controls"));
    if (!input) {
        return;
    }

    const show = input.type === "password";
    input.type = show ? "text" : "password";
    button.textContent = show ? "Ẩn" : "Hiện";
    button.setAttribute("aria-label", show ? "Ẩn mật khẩu" : "Hiện mật khẩu");
});
