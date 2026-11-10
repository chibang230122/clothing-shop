package com.example.clothingshop.network;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/** Kiểm thử bộ đọc dòng có giới hạn độ dài (không cần database hay Spring). */
class BoundedLineReaderTests {

    private static BoundedLineReader reader(String text, int max) {
        return new BoundedLineReader(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), max);
    }

    @Test
    void readsLinesEndedByLfAndCrLf() throws IOException {
        BoundedLineReader r = reader("PING\nCHECK_STOCK AT-DEN-M\r\n", 64);
        assertEquals("PING", r.readLine());
        assertEquals("CHECK_STOCK AT-DEN-M", r.readLine());
        assertNull(r.readLine());
    }

    @Test
    void returnsLastLineEvenWithoutNewline() throws IOException {
        BoundedLineReader r = reader("PING", 64);
        assertEquals("PING", r.readLine());
        assertNull(r.readLine());
    }

    @Test
    void decodesVietnameseText() throws IOException {
        BoundedLineReader r = reader("Áo thun đen\n", 64);
        assertEquals("Áo thun đen", r.readLine());
    }

    @Test
    void throwsWhenLineIsLongerThanLimit() {
        BoundedLineReader r = reader("A".repeat(100) + "\n", 32);
        assertThrows(LineTooLongException.class, r::readLine);
    }

    @Test
    void acceptsLineExactlyAtLimit() throws IOException {
        BoundedLineReader r = reader("A".repeat(32) + "\n", 32);
        assertEquals(32, r.readLine().length());
    }
}
