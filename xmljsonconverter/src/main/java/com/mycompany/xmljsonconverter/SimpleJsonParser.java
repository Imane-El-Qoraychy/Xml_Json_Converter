package com.mycompany.xmljsonconverter;

import java.util.*;

public class SimpleJsonParser {

    private final String s;
    private int i = 0;

    public SimpleJsonParser(String json) {
        this.s = (json == null) ? "" : json.trim();
    }

    public Object parse() {
        skipWs();
        Object v = parseValue();
        skipWs();
        if (i != s.length()) {
            throw new IllegalArgumentException("Extra characters at position " + i);
        }
        return v;
    }

    private Object parseValue() {
        skipWs();
        if (i >= s.length()) throw new IllegalArgumentException("Unexpected end of JSON");

        char c = s.charAt(i);

        if (c == '{') return parseObject();
        if (c == '[') return parseArray();
        if (c == '"') return parseString();
        if (c == 't') { expect("true"); return true; }
        if (c == 'f') { expect("false"); return false; }
        if (c == 'n') { expect("null"); return null; }

        if (c == '-' || Character.isDigit(c)) return parseNumber();

        throw new IllegalArgumentException("Unexpected char '" + c + "' at position " + i);
    }

    private Map<String, Object> parseObject() {
        Map<String, Object> map = new LinkedHashMap<>();
        expectChar('{');
        skipWs();

        if (peek('}')) { i++; return map; }

        while (true) {
            skipWs();
            if (!peek('"')) throw new IllegalArgumentException("Expected string key at position " + i);

            String key = parseString();

            skipWs();
            expectChar(':');

            Object value = parseValue();
            map.put(key, value);

            skipWs();
            if (peek('}')) { i++; break; }
            expectChar(',');
        }
        return map;
    }

    private List<Object> parseArray() {
        List<Object> list = new ArrayList<>();
        expectChar('[');
        skipWs();

        if (peek(']')) { i++; return list; }

        while (true) {
            Object v = parseValue();
            list.add(v);

            skipWs();
            if (peek(']')) { i++; break; }
            expectChar(',');
        }
        return list;
    }

    private String parseString() {
        expectChar('"');
        StringBuilder sb = new StringBuilder();

        while (i < s.length()) {
            char c = s.charAt(i++);
            if (c == '"') return sb.toString();

            if (c == '\\') {
                if (i >= s.length()) throw new IllegalArgumentException("Bad escape at end");
                char e = s.charAt(i++);
                switch (e) {
                    case '"', '\\', '/' -> sb.append(e);
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'u' -> {
                        if (i + 4 > s.length()) throw new IllegalArgumentException("Bad unicode escape");
                        String hex = s.substring(i, i + 4);
                        i += 4;
                        sb.append((char) Integer.parseInt(hex, 16));
                    }
                    default -> throw new IllegalArgumentException("Bad escape: \\" + e);
                }
            } else {
                sb.append(c);
            }
        }

        throw new IllegalArgumentException("Unterminated string");
    }

    private Number parseNumber() {
        int start = i;

        if (peek('-')) i++;
        int digitsStart = i;

        while (i < s.length() && Character.isDigit(s.charAt(i))) i++;

        boolean isFloat = false;
        if (peek('.')) {
            isFloat = true;
            i++;
            while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
        }

        // optional exponent
        if (peek('e') || peek('E')) {
            isFloat = true;
            i++;
            if (peek('+') || peek('-')) i++;
            while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
        }

        String num = s.substring(start, i);

        if (digitsStart == i || num.equals("-")) {
            throw new IllegalArgumentException("Invalid number at position " + start);
        }

        try {
            if (isFloat) return Double.parseDouble(num);
            long L = Long.parseLong(num);
            if (L >= Integer.MIN_VALUE && L <= Integer.MAX_VALUE) return (int) L;
            return L;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid number: " + num);
        }
    }

    private void skipWs() {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
    }

    private boolean peek(char c) {
        return i < s.length() && s.charAt(i) == c;
    }

    private void expect(String token) {
        if (!s.startsWith(token, i)) {
            throw new IllegalArgumentException("Expected '" + token + "' at position " + i);
        }
        i += token.length();
    }

    private void expectChar(char c) {
        if (i >= s.length() || s.charAt(i) != c) {
            throw new IllegalArgumentException("Expected '" + c + "' at position " + i);
        }
        i++;
    }
}
