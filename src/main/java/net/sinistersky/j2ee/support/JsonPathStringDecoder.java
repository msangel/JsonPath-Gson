package net.sinistersky.j2ee.support;

final class JsonPathStringDecoder {

    private JsonPathStringDecoder() {
    }

    static String unquote(String value) {
        StringBuilder result = new StringBuilder();
        for (int index = 1; index < value.length() - 1; index++) {
            char current = value.charAt(index);
            if (current != '\\') {
                result.append(current);
                continue;
            }
            char escaped = value.charAt(++index);
            switch (escaped) {
                case 'b':
                    result.append('\b');
                    break;
                case 'f':
                    result.append('\f');
                    break;
                case 'n':
                    result.append('\n');
                    break;
                case 'r':
                    result.append('\r');
                    break;
                case 't':
                    result.append('\t');
                    break;
                case 'u':
                    index = appendUnicodeEscape(value, index + 1, result);
                    break;
                default:
                    result.append(escaped);
                    break;
            }
        }
        return result.toString();
    }

    private static int appendUnicodeEscape(String value, int hexStart, StringBuilder result) {
        char first = (char) Integer.parseInt(value.substring(hexStart, hexStart + 4), 16);
        int lastConsumed = hexStart + 3;
        if (Character.isHighSurrogate(first)) {
            int lowStart = hexStart + 6;
            char second = (char) Integer.parseInt(value.substring(lowStart, lowStart + 4), 16);
            result.appendCodePoint(Character.toCodePoint(first, second));
            lastConsumed = lowStart + 3;
        } else {
            result.append(first);
        }
        return lastConsumed;
    }
}
