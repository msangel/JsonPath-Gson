package net.sinistersky.j2ee.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class IRegexpTest {

    @ParameterizedTest(name = "I-Regexp {0} against {1}: full={2}, expected={3}")
    @MethodSource("matchCases")
    void implementsIRegexpMatching(String expression, String input,
            boolean fullMatch, boolean expected) {
        Pattern pattern = IRegexp.compile(expression);

        assertNotNull(pattern);
        boolean result = fullMatch
                ? pattern.matcher(input).matches() : pattern.matcher(input).find();
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "rejects invalid I-Regexp {0}")
    @ValueSource(strings = {
            "(",
            ")",
            "*a",
            "a++",
            "[]",
            "[^]",
            "[a-\\p{L}]",
            "\\d",
            "\\w",
            "\\p{BasicLatin}",
            "(?=a)",
            "a{,2}",
            "a{2,1}",
            "a{2",
            "a}",
            "\uD800"
    })
    void rejectsExpressionsOutsideIRegexp(String expression) {
        assertNull(IRegexp.compile(expression));
    }

    private static Stream<Arguments> matchCases() {
        return Stream.of(
                Arguments.of("", "", true, true),
                Arguments.of("", "a", true, false),
                Arguments.of("", "a", false, true),
                Arguments.of("a|b", "b", true, true),
                Arguments.of("(ab){2,3}", "ababab", true, true),
                Arguments.of("(ab){2,3}", "ab", true, false),
                Arguments.of("a.b", "a\nb", true, false),
                Arguments.of("a.b", "a\rb", true, false),
                Arguments.of("a.b", "a\u2028b", true, true),
                Arguments.of("\\.", ".", true, true),
                Arguments.of("$^", "$^", true, true),
                Arguments.of("[a-z]+", "jsonpath", true, true),
                Arguments.of("[-]+", "---", true, true),
                Arguments.of("[a-]+", "a--", true, true),
                Arguments.of("[^a]+", "bbb", true, true),
                Arguments.of("[&&]+", "&&", true, true),
                Arguments.of("\\p{L}+", "Україна", true, true),
                Arguments.of("\\p{L}+", "9535", true, false),
                Arguments.of("\\P{L}+", "9535", true, true),
                Arguments.of("[\\p{Nd}]+", "١٢", true, true),
                Arguments.of("[jk]", "kilo", true, false),
                Arguments.of("[jk]", "kilo", false, true));
    }
}
