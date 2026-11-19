package aiAssistance;

import java.util.*;

/*******
 * <p> Title: JsonCodec Class. </p>
 * <p> Description: Small strict JSON codec for the provider wire format, avoiding an additional
 * application dependency. Input size and nesting are bounded. Validated by OpenAiClientTest's
 * escaping, Unicode, malformed response and request-shape tests.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Provider JSON
 */
final class JsonCodec {
    private final String input;
    private int at;
    private JsonCodec(String input) { this.input = input; }
    static Object parse(String input) {
        if (input == null || input.length() > 262144) throw new IllegalArgumentException("Invalid JSON size");
        var parser = new JsonCodec(input); Object result = parser.value(0); parser.space();
        if (parser.at != input.length()) throw new IllegalArgumentException("Trailing JSON"); return result;
    }
    static String encode(Object value) {
        if (value == null) return "null";
        if (value instanceof String text) {
            StringBuilder out = new StringBuilder("\"");
            for (char c : text.toCharArray()) {
                if (c == '"' || c == '\\') out.append('\\').append(c);
                else if (c < 32 || Character.isSurrogate(c)) out.append(String.format("\\u%04x", (int)c));
                else out.append(c);
            }
            return out.append('"').toString();
        }
        if (value instanceof Boolean || value instanceof Number) return value.toString();
        if (value instanceof List<?> list) return "[" + String.join(",", list.stream().map(JsonCodec::encode).toList()) + "]";
        if (value instanceof Map<?, ?> map) return "{" + String.join(",", map.entrySet().stream()
                .map(entry -> encode(entry.getKey().toString()) + ":" + encode(entry.getValue())).toList()) + "}";
        throw new IllegalArgumentException("Unsupported JSON value");
    }
    private Object value(int depth) {
        if (depth > 64) throw new IllegalArgumentException("JSON nesting too deep"); space();
        if (at >= input.length()) throw new IllegalArgumentException("Missing JSON value");
        char c = input.charAt(at);
        if (c == '"') return string();
        if (c == '{') {
            at++; Map<String, Object> map = new LinkedHashMap<>(); space(); if (take('}')) return map;
            do {
                space(); String key = string(); space(); require(':');
                if (map.containsKey(key)) throw new IllegalArgumentException("Duplicate JSON key");
                map.put(key, value(depth + 1)); space(); if (take('}')) return map; require(',');
            } while (true);
        }
        if (c == '[') {
            at++; List<Object> list = new ArrayList<>(); space(); if (take(']')) return list;
            do { list.add(value(depth + 1)); space(); if (take(']')) return list; require(','); } while (true);
        }
        for (String literal : List.of("true", "false", "null")) if (input.startsWith(literal, at)) {
            at += literal.length(); return literal.equals("null") ? null : Boolean.valueOf(literal);
        }
        int start = at;
        if (take('-') && at == input.length()) throw new IllegalArgumentException("Invalid number");
        if (!take('0')) { if (at >= input.length() || input.charAt(at) < '1' || input.charAt(at) > '9') throw new IllegalArgumentException("Invalid number"); digits(); }
        if (take('.')) { int before = at; digits(); if (before == at) throw new IllegalArgumentException("Invalid fraction"); }
        if (take('e') || take('E')) { if (!take('+')) take('-'); int before = at; digits(); if (before == at) throw new IllegalArgumentException("Invalid exponent"); }
        return new java.math.BigDecimal(input.substring(start, at));
    }
    private String string() {
        require('"'); StringBuilder text = new StringBuilder();
        while (at < input.length()) {
            char c = input.charAt(at++); if (c == '"') return text.toString();
            if (c < 32) throw new IllegalArgumentException("Control character in JSON");
            if (c != '\\') { text.append(c); continue; }
            if (at >= input.length()) throw new IllegalArgumentException("Invalid escape");
            char escape = input.charAt(at++);
            switch (escape) {
            case '"', '\\', '/' -> text.append(escape);
            case 'b' -> text.append('\b'); case 'f' -> text.append('\f'); case 'n' -> text.append('\n'); case 'r' -> text.append('\r'); case 't' -> text.append('\t');
            case 'u' -> { if (at + 4 > input.length()) throw new IllegalArgumentException("Invalid Unicode escape"); String hex = input.substring(at, at + 4); if (!hex.matches("[0-9a-fA-F]{4}")) throw new IllegalArgumentException("Invalid Unicode escape"); text.append((char)Integer.parseInt(hex, 16)); at += 4; }
            default -> throw new IllegalArgumentException("Invalid JSON escape");
            }
        }
        throw new IllegalArgumentException("Unterminated JSON string");
    }
    private void digits() { while (at < input.length() && input.charAt(at) >= '0' && input.charAt(at) <= '9') at++; }
    private void space() { while (at < input.length() && " \t\r\n".indexOf(input.charAt(at)) >= 0) at++; }
    private boolean take(char c) { if (at < input.length() && input.charAt(at) == c) { at++; return true; } return false; }
    private void require(char c) { if (!take(c)) throw new IllegalArgumentException("Malformed JSON"); }
}
