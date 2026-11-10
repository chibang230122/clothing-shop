package com.example.clothingshop.network;

import java.io.IOException;

/** Ném ra khi client gửi một dòng dài hơn giới hạn cho phép. */
public class LineTooLongException extends IOException {

    private static final long serialVersionUID = 1L;

    public LineTooLongException(int maxBytes) {
        super("Dòng request vượt quá " + maxBytes + " byte.");
    }
}
