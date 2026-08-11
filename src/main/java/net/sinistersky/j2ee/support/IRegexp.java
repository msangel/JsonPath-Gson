package net.sinistersky.j2ee.support;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

final class IRegexp {

    private static final Set<String> CHARACTER_PROPERTIES = new HashSet<>(Arrays.asList(
            "L", "Ll", "Lm", "Lo", "Lt", "Lu",
            "M", "Mc", "Me", "Mn",
            "N", "Nd", "Nl", "No",
            "P", "Pc", "Pd", "Pe", "Pf", "Pi", "Po", "Ps",
            "Z", "Zl", "Zp", "Zs",
            "S", "Sc", "Sk", "Sm", "So",
            "C", "Cc", "Cf", "Cn", "Co"));

    private IRegexp() {
    }

    static Pattern compile(String expression) {
        try {
            return Pattern.compile(new Parser(expression).translate());
        } catch (InvalidIRegexpException | PatternSyntaxException exception) {
            return null;
        }
    }

    private static final class Parser {
        private final String source;
        private int offset;

        private Parser(String source) {
            this.source = source;
        }

        private String translate() {
            StringBuilder result = new StringBuilder();
            parseRegularExpression(result);
            if (offset != source.length()) {
                invalid();
            }
            return result.toString();
        }

        private void parseRegularExpression(StringBuilder result) {
            parseBranch(result);
            while (take('|')) {
                result.append('|');
                parseBranch(result);
            }
        }

        private void parseBranch(StringBuilder result) {
            while (offset < source.length() && peek() != '|' && peek() != ')') {
                parseAtom(result);
                parseQuantifier(result);
            }
        }

        private void parseAtom(StringBuilder result) {
            if (take('(')) {
                result.append("(?:");
                parseRegularExpression(result);
                require(')');
                result.append(')');
                return;
            }
            if (peek() == '.') {
                offset++;
                result.append("[^\\n\\r]");
                return;
            }
            if (peek() == '[') {
                parseCharacterClass(result);
                return;
            }
            if (peek() == '\\') {
                if (isCharacterProperty()) {
                    parseCharacterProperty(result);
                } else {
                    parseSingleCharacterEscape(result);
                }
                return;
            }
            appendNormalCharacter(result);
        }

        private void parseQuantifier(StringBuilder result) {
            if (offset == source.length()) {
                return;
            }
            char current = peek();
            if (current == '*' || current == '+' || current == '?') {
                result.append(current);
                offset++;
                return;
            }
            if (current != '{') {
                return;
            }
            result.append('{');
            offset++;
            appendDigits(result);
            if (take(',')) {
                result.append(',');
                if (offset < source.length() && isAsciiDigit(peek())) {
                    appendDigits(result);
                }
            }
            require('}');
            result.append('}');
        }

        private void appendDigits(StringBuilder result) {
            int start = offset;
            while (offset < source.length() && isAsciiDigit(peek())) {
                result.append(peek());
                offset++;
            }
            if (offset == start) {
                invalid();
            }
        }

        private void parseCharacterClass(StringBuilder result) {
            require('[');
            result.append('[');
            if (take('^')) {
                result.append('^');
            }
            if (offset == source.length() || peek() == ']') {
                invalid();
            }
            if (take('-')) {
                result.append("\\-");
            } else {
                parseCharacterClassElement(result);
            }
            while (offset < source.length() && peek() != ']') {
                if (peek() == '-' && offset + 1 < source.length()
                        && source.charAt(offset + 1) == ']') {
                    offset++;
                    result.append("\\-");
                    break;
                }
                parseCharacterClassElement(result);
            }
            require(']');
            result.append(']');
        }

        private void parseCharacterClassElement(StringBuilder result) {
            if (isCharacterProperty()) {
                parseCharacterProperty(result);
                return;
            }
            appendCharacterClassCharacter(result);
            if (offset + 1 < source.length() && peek() == '-'
                    && source.charAt(offset + 1) != ']') {
                offset++;
                result.append('-');
                appendCharacterClassCharacter(result);
            }
        }

        private void appendCharacterClassCharacter(StringBuilder result) {
            if (offset == source.length()) {
                invalid();
            }
            if (peek() == '\\') {
                parseSingleCharacterEscape(result);
                return;
            }
            int codePoint = readScalarValue();
            if (codePoint == '-' || codePoint == '[' || codePoint == '\\'
                    || codePoint == ']') {
                invalid();
            }
            if (codePoint == '^' || codePoint == '&') {
                result.append('\\');
            }
            result.appendCodePoint(codePoint);
        }

        private void appendNormalCharacter(StringBuilder result) {
            int codePoint = readScalarValue();
            if (codePoint == '(' || codePoint == ')' || codePoint == '*'
                    || codePoint == '+' || codePoint == '.' || codePoint == '?'
                    || codePoint == '[' || codePoint == '\\' || codePoint == ']'
                    || codePoint == '{' || codePoint == '|' || codePoint == '}') {
                invalid();
            }
            if (codePoint == '$' || codePoint == '^') {
                result.append('\\');
            }
            result.appendCodePoint(codePoint);
        }

        private void parseSingleCharacterEscape(StringBuilder result) {
            require('\\');
            if (offset == source.length()) {
                invalid();
            }
            char escaped = source.charAt(offset++);
            if (!isSingleCharacterEscape(escaped)) {
                invalid();
            }
            result.append('\\').append(escaped);
        }

        private boolean isCharacterProperty() {
            return offset + 2 < source.length() && peek() == '\\'
                    && (source.charAt(offset + 1) == 'p'
                    || source.charAt(offset + 1) == 'P')
                    && source.charAt(offset + 2) == '{';
        }

        private void parseCharacterProperty(StringBuilder result) {
            require('\\');
            char type = source.charAt(offset++);
            require('{');
            int start = offset;
            while (offset < source.length() && source.charAt(offset) != '}') {
                offset++;
            }
            if (offset == source.length()) {
                invalid();
            }
            String property = source.substring(start, offset);
            if (!CHARACTER_PROPERTIES.contains(property)) {
                invalid();
            }
            offset++;
            result.append('\\').append(type).append('{').append(property).append('}');
        }

        private int readScalarValue() {
            if (offset == source.length()) {
                invalid();
            }
            char first = source.charAt(offset);
            if (Character.isHighSurrogate(first)) {
                if (offset + 1 >= source.length()
                        || !Character.isLowSurrogate(source.charAt(offset + 1))) {
                    invalid();
                }
                int result = Character.toCodePoint(first, source.charAt(offset + 1));
                offset += 2;
                return result;
            }
            if (Character.isLowSurrogate(first)) {
                invalid();
            }
            offset++;
            return first;
        }

        private boolean take(char expected) {
            if (offset < source.length() && source.charAt(offset) == expected) {
                offset++;
                return true;
            }
            return false;
        }

        private void require(char expected) {
            if (!take(expected)) {
                invalid();
            }
        }

        private char peek() {
            if (offset == source.length()) {
                invalid();
            }
            return source.charAt(offset);
        }

        private static boolean isSingleCharacterEscape(char value) {
            return value >= '(' && value <= '+' || value == '-' || value == '.'
                    || value == '?' || value >= '[' && value <= '^'
                    || value == 'n' || value == 'r' || value == 't'
                    || value >= '{' && value <= '}';
        }

        private static boolean isAsciiDigit(char value) {
            return value >= '0' && value <= '9';
        }

        private static void invalid() {
            throw new InvalidIRegexpException();
        }
    }

    private static final class InvalidIRegexpException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
