package org.north.core.system.remote;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimalistic JSON reader and writer.
 *
 * <p>The engine does not depend on any third party JSON library, and the protocol of the remote
 * gamepad only needs flat objects with primitive values, therefore a small hand written
 * implementation is used instead of pulling a dependency into the core module.</p>
 */
public final class Json {

    private Json() {
    }

    /**
     * Parses a JSON text into a {@link Map}, {@link List}, {@link String}, {@link Double},
     * {@link Boolean} or {@code null}.
     */
    public static Object parse(String text) {
        if (text == null) throw new IllegalArgumentException("JSON text is null");
        Parser parser = new Parser(text);
        parser.skipWhitespace();
        Object result = parser.readValue();
        parser.skipWhitespace();
        if (!parser.atEnd()) throw new IllegalArgumentException("Unexpected trailing content in JSON text");
        return result;
    }

    /**
     * Parses a JSON text and expects an object as the root element.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        Object value = parse(text);
        if (!(value instanceof Map)) throw new IllegalArgumentException("Root element of JSON text is not an object");
        return (Map<String, Object>) value;
    }

    /**
     * Serializes a value into a JSON text.
     */
    public static String write(Object value) {
        StringBuilder builder = new StringBuilder();
        writeValue(builder, value);
        return builder.toString();
    }

    public static String getString(Map<String, Object> object, String key, String fallback) {
        Object value = object == null ? null : object.get(key);
        return value instanceof String ? (String) value : fallback;
    }

    public static double getDouble(Map<String, Object> object, String key, double fallback) {
        Object value = object == null ? null : object.get(key);
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
        return fallback;
    }

    public static boolean getBoolean(Map<String, Object> object, String key, boolean fallback) {
        Object value = object == null ? null : object.get(key);
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) return Boolean.parseBoolean((String) value);
        if (value instanceof Number) return ((Number) value).doubleValue() != 0d;
        return fallback;
    }

    // ------------------------------------------------------------------
    // Writing
    // ------------------------------------------------------------------

    private static void writeValue(StringBuilder builder, Object value) {
        if (value == null) {
            builder.append("null");
        } else if (value instanceof Map) {
            builder.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (!first) builder.append(',');
                first = false;
                writeString(builder, String.valueOf(entry.getKey()));
                builder.append(':');
                writeValue(builder, entry.getValue());
            }
            builder.append('}');
        } else if (value instanceof Iterable) {
            builder.append('[');
            boolean first = true;
            for (Object element : (Iterable<?>) value) {
                if (!first) builder.append(',');
                first = false;
                writeValue(builder, element);
            }
            builder.append(']');
        } else if (value instanceof Boolean) {
            builder.append(value.toString());
        } else if (value instanceof Number) {
            writeNumber(builder, (Number) value);
        } else {
            writeString(builder, value.toString());
        }
    }

    private static void writeNumber(StringBuilder builder, Number number) {
        double doubleValue = number.doubleValue();
        if (number instanceof Float || number instanceof Double) {
            if (Double.isNaN(doubleValue) || Double.isInfinite(doubleValue)) {
                builder.append('0');
                return;
            }
            if (doubleValue == Math.rint(doubleValue) && Math.abs(doubleValue) < 1e15) {
                builder.append((long) doubleValue);
                return;
            }
            builder.append(doubleValue);
            return;
        }
        builder.append(number.toString());
    }

    private static void writeString(StringBuilder builder, String value) {
        builder.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':  builder.append("\\\""); break;
                case '\\': builder.append("\\\\"); break;
                case '\b': builder.append("\\b"); break;
                case '\f': builder.append("\\f"); break;
                case '\n': builder.append("\\n"); break;
                case '\r': builder.append("\\r"); break;
                case '\t': builder.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        builder.append(String.format("\\u%04x", (int) c));
                    } else {
                        builder.append(c);
                    }
                    break;
            }
        }
        builder.append('"');
    }

    // ------------------------------------------------------------------
    // Parsing
    // ------------------------------------------------------------------

    private static final class Parser {
        private final String source;
        private int position;

        private Parser(String source) {
            this.source = source;
        }

        private boolean atEnd() {
            return position >= source.length();
        }

        private void skipWhitespace() {
            while (position < source.length() && Character.isWhitespace(source.charAt(position))) {
                position++;
            }
        }

        private Object readValue() {
            skipWhitespace();
            if (atEnd()) throw new IllegalArgumentException("Unexpected end of JSON text");

            char c = source.charAt(position);
            switch (c) {
                case '{': return readObject();
                case '[': return readArray();
                case '"': return readString();
                case 't': readLiteral("true"); return Boolean.TRUE;
                case 'f': readLiteral("false"); return Boolean.FALSE;
                case 'n': readLiteral("null"); return null;
                default:  return readNumber();
            }
        }

        private Map<String, Object> readObject() {
            Map<String, Object> object = new LinkedHashMap<>();
            position++; // '{'
            skipWhitespace();
            if (!atEnd() && source.charAt(position) == '}') {
                position++;
                return object;
            }

            while (true) {
                skipWhitespace();
                String key = readString();
                skipWhitespace();
                expect(':');
                object.put(key, readValue());
                skipWhitespace();

                if (atEnd()) throw new IllegalArgumentException("Unterminated JSON object");
                char c = source.charAt(position++);
                if (c == '}') return object;
                if (c != ',') throw new IllegalArgumentException("Unexpected character '" + c + "' in JSON object");
            }
        }

        private List<Object> readArray() {
            List<Object> array = new ArrayList<>();
            position++; // '['
            skipWhitespace();
            if (!atEnd() && source.charAt(position) == ']') {
                position++;
                return array;
            }

            while (true) {
                array.add(readValue());
                skipWhitespace();

                if (atEnd()) throw new IllegalArgumentException("Unterminated JSON array");
                char c = source.charAt(position++);
                if (c == ']') return array;
                if (c != ',') throw new IllegalArgumentException("Unexpected character '" + c + "' in JSON array");
            }
        }

        private String readString() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (true) {
                if (atEnd()) throw new IllegalArgumentException("Unterminated JSON string");
                char c = source.charAt(position++);
                if (c == '"') return builder.toString();
                if (c != '\\') {
                    builder.append(c);
                    continue;
                }
                if (atEnd()) throw new IllegalArgumentException("Unterminated JSON escape sequence");
                char escaped = source.charAt(position++);
                switch (escaped) {
                    case '"':  builder.append('"'); break;
                    case '\\': builder.append('\\'); break;
                    case '/':  builder.append('/'); break;
                    case 'b':  builder.append('\b'); break;
                    case 'f':  builder.append('\f'); break;
                    case 'n':  builder.append('\n'); break;
                    case 'r':  builder.append('\r'); break;
                    case 't':  builder.append('\t'); break;
                    case 'u':
                        if (position + 4 > source.length()) throw new IllegalArgumentException("Invalid unicode escape");
                        builder.append((char) Integer.parseInt(source.substring(position, position + 4), 16));
                        position += 4;
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid escape sequence '\\" + escaped + "' in JSON string");
                }
            }
        }

        private Double readNumber() {
            int start = position;
            while (!atEnd()) {
                char c = source.charAt(position);
                if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
                    position++;
                } else {
                    break;
                }
            }
            if (start == position) throw new IllegalArgumentException("Invalid JSON number");
            try {
                return Double.parseDouble(source.substring(start, position));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid JSON number: " + source.substring(start, position), e);
            }
        }

        private void readLiteral(String literal) {
            if (!source.startsWith(literal, position)) {
                throw new IllegalArgumentException("Invalid JSON literal at position " + position);
            }
            position += literal.length();
        }

        private void expect(char expected) {
            if (atEnd() || source.charAt(position) != expected) {
                throw new IllegalArgumentException("Expected '" + expected + "' in JSON text at position " + position);
            }
            position++;
        }
    }
}
