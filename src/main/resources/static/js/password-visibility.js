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

for (const input of document.querySelectorAll("[data-password-rule]")) {
    const validate = () => {
        const value = input.value;
        const valid = value.length >= 8 && value.length <= 50
            && /\p{Lu}/u.test(value) && /\p{Ll}/u.test(value)
            && /\p{Nd}/u.test(value) && /[\p{P}\p{S}]/u.test(value)
            && new TextEncoder().encode(value).length <= 72;
        input.setCustomValidity(valid ? "" : input.dataset.passwordRule);
    };
    input.addEventListener("input", validate);
    input.addEventListener("change", validate);
    input.addEventListener("invalid", validate);
}
