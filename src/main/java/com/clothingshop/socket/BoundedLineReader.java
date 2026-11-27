package com.clothingshop.socket;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class BoundedLineReader {

    public static class LineTooLongException extends IOException {
        public LineTooLongException(int maxBytes) {
            super("Line exceeded max length of " + maxBytes + " bytes");
        }
    }

    private final InputStream in;
    private final int maxBytes;

    public BoundedLineReader(InputStream in, int maxBytes) {
        this.in = (in instanceof BufferedInputStream) ? in : new BufferedInputStream(in);
        this.maxBytes = maxBytes;
    }

    public String readLine() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(128);
        boolean readAnything = false;
        int b;
        while ((b = in.read()) != -1) {
            readAnything = true;
            if (b == '\n') {
                return buffer.toString(StandardCharsets.UTF_8);
            }
            if (b == '\r') {
                continue;
            }
            if (buffer.size() >= maxBytes) {
                throw new LineTooLongException(maxBytes);
            }
            buffer.write(b);
        }
        return (readAnything && buffer.size() > 0) ? buffer.toString(StandardCharsets.UTF_8) : null;
    }
}