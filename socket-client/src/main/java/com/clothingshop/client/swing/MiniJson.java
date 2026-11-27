package com.clothingshop.client.swing;

import java.util.*;

public final class MiniJson {

    private MiniJson() {}

    public static String stringify(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String s) return "\"" + escape(s) + "\"";
        if (obj instanceof Number || obj instanceof Boolean) return obj.toString();
        if (obj instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append('"').append(escape(String.valueOf(e.getKey()))).append("\":").append(stringify(e.getValue()));
            }
            return sb.append('}').toString();
        }
        if (obj instanceof Iterable<?> list) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : list) {
                if (!first) sb.append(',');
                first = false;
                sb.append(stringify(item));
            }
            return sb.append(']').toString();
        }
        return "\"" + escape(obj.toString()) + "\"";
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String json) {
        Object val = new Parser(json).parseValue();
        if (val instanceof Map<?, ?> m) return (Map<String, Object>) m;
        throw new IllegalArgumentException("JSON không phải Object");
    }

    private static final class Parser {
        private final String src;
        private int pos = 0;

        Parser(String src) { this.src = src; }

        Object parseValue() {
            skipWs();
            if (pos >= src.length()) return null;
            char c = src.charAt(pos);
            return switch (c) {
                case '{' -> parseObj();
                case '[' -> parseArr();
                case '"' -> parseStr();
                case 't' -> { pos += 4; yield Boolean.TRUE; }
                case 'f' -> { pos += 5; yield Boolean.FALSE; }
                case 'n' -> { pos += 4; yield null; }
                default -> parseNum();
            };
        }

        Map<String, Object> parseObj() {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++;
            skipWs();
            if (peek() == '}') { pos++; return map; }
            while (pos < src.length()) {
                skipWs();
                String key = parseStr();
                skipWs();
                pos++;
                Object val = parseValue();
                map.put(key, val);
                skipWs();
                char ch = src.charAt(pos++);
                if (ch == '}') break;
            }
            return map;
        }

        List<Object> parseArr() {
            List<Object> list = new ArrayList<>();
            pos++;
            skipWs();
            if (peek() == ']') { pos++; return list; }
            while (pos < src.length()) {
                list.add(parseValue());
                skipWs();
                char ch = src.charAt(pos++);
                if (ch == ']') break;
            }
            return list;
        }

        String parseStr() {
            pos++;
            StringBuilder sb = new StringBuilder();
            while (pos < src.length()) {
                char c = src.charAt(pos++);
                if (c == '"') break;
                if (c == '\\') {
                    char esc = src.charAt(pos++);
                    switch (esc) {
                        case '"', '\\', '/' -> sb.append(esc);
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            String hex = src.substring(pos, pos + 4);
                            sb.append((char) Integer.parseInt(hex, 16));
                            pos += 4;
                        }
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        Object parseNum() {
            int start = pos;
            while (pos < src.length() && "-0123456789.eE+".indexOf(src.charAt(pos)) >= 0) pos++;
            String token = src.substring(start, pos);
            if (token.contains(".") || token.contains("e") || token.contains("E")) {
                return Double.parseDouble(token);
            }
            long l = Long.parseLong(token);
            return (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) ? (int) l : l;
        }

        void skipWs() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
        }

        char peek() { return pos < src.length() ? src.charAt(pos) : '\0'; }
    }
}
