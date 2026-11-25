package com.example.clothingshop.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyTests {
    @ParameterizedTest
    @ValueSource(strings = {"short1!", "lowercase1!", "UPPERCASE1!", "MissingDigits!", "MissingSymbol1", "SpaceOnly1 "})
    void rejectsMissingRequirements(String password) {
        assertFalse(PasswordPolicy.accepts(password));
    }

    @Test
    void checksLengthAndUtf8ByteLimit() {
        assertTrue(PasswordPolicy.accepts("Đẹp1234!"));
        assertTrue(PasswordPolicy.accepts("Aa1!" + "a".repeat(46)));
        assertFalse(PasswordPolicy.accepts("Aa1!" + "a".repeat(47)));
        assertFalse(PasswordPolicy.accepts("Aa1!" + "ệ".repeat(23)));
    }
}
