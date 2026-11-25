package com.example.clothingshop.service;

import java.nio.charset.StandardCharsets;

public final class PasswordPolicy {
    public static final String GUIDE = "Mật khẩu cần 8–50 ký tự, có chữ hoa, chữ thường, chữ số và ký hiệu đặc biệt";

    private PasswordPolicy() {}

    public static boolean accepts(String value) {
        if (value == null || value.length() < 8 || value.length() > 50
                || value.getBytes(StandardCharsets.UTF_8).length > 72) {
            return false;
        }
        return value.codePoints().anyMatch(Character::isUpperCase)
                && value.codePoints().anyMatch(Character::isLowerCase)
                && value.codePoints().anyMatch(Character::isDigit)
                && value.matches("(?s).*[\\p{P}\\p{S}].*");
    }

    public static void require(String value) {
        if (!accepts(value)) {
            throw new IllegalArgumentException(GUIDE);
        }
    }
}
