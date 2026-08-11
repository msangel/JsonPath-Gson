package net.sinistersky.j2ee.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.sinistersky.j2ee.support.nodetypes.ArrayIndexPathNode;
import net.sinistersky.j2ee.support.nodetypes.CSVIndexPathNode;
import net.sinistersky.j2ee.support.nodetypes.NamedPropertyPathNode;
import net.sinistersky.j2ee.support.nodetypes.PathNode;
import net.sinistersky.j2ee.support.nodetypes.RecursiveDescentPathNode;
import net.sinistersky.j2ee.support.nodetypes.SlicePathNode;
import net.sinistersky.j2ee.support.nodetypes.WildcardPathNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class AntlrParserTest {

    private final AntlrParser parser = new AntlrParser();

    @ParameterizedTest
    @ValueSource(strings = {"$.store..book['title'][0][1,-2][1:9:2][*].*"})
    void createsPathNodesForEverySupportedSelector(String path) {
        List<PathNode> nodes = parser.parseExpression(path).getNodes();

        assertEquals(9, nodes.size());
        assertInstanceOf(NamedPropertyPathNode.class, nodes.get(0));
        assertInstanceOf(RecursiveDescentPathNode.class, nodes.get(1));
        assertInstanceOf(NamedPropertyPathNode.class, nodes.get(2));
        assertInstanceOf(NamedPropertyPathNode.class, nodes.get(3));
        assertInstanceOf(ArrayIndexPathNode.class, nodes.get(4));
        assertInstanceOf(CSVIndexPathNode.class, nodes.get(5));
        assertInstanceOf(SlicePathNode.class, nodes.get(6));
        assertInstanceOf(WildcardPathNode.class, nodes.get(7));
        assertInstanceOf(WildcardPathNode.class, nodes.get(8));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("executionCases")
    void executesParsedExpression(String json, String path, String expectedValues) {
        List<JsonElement> result = parser.parseExpression(path).exec(json);

        assertEquals(expectedValues, joinValues(result));
    }

    @ParameterizedTest(name = "root-only query returns {0}")
    @ValueSource(strings = {"{'value':42}", "[1,2,3]", "true"})
    void executesRootOnlyQuery(String json) {
        Expression expression = parser.parseExpression("$");

        assertEquals(0, expression.getNodes().size());
        assertEquals(Collections.singletonList(JsonParser.parseString(json)), expression.exec(json));
    }

    @ParameterizedTest(name = "RFC member name [{0}]")
    @MethodSource("rfcMemberNames")
    void supportsRfcUnicodeMemberNameShorthand(String memberName) {
        JsonObject json = new JsonObject();
        json.addProperty(memberName, 42);

        List<JsonElement> result = parser.parseExpression("$." + memberName).exec(json);

        assertEquals(Collections.singletonList(JsonParser.parseString("42")), result);
    }

    @ParameterizedTest(name = "decodes {0}")
    @MethodSource("quotedPropertyCases")
    void unescapesQuotedPropertyNames(String path, String propertyName) {
        JsonObject json = new JsonObject();
        json.addProperty(propertyName, 42);

        List<JsonElement> result = parser.parseExpression(path).exec(json);

        assertEquals(42, result.get(0).getAsInt());
    }

    @ParameterizedTest(name = "rejects [{0}]")
    @MethodSource("invalidExpressions")
    void rejectsInvalidInputAsJsonPathException(String path) {
        assertThrows(JsonPathException.class, () -> parser.parseExpression(path));
    }

    @ParameterizedTest(name = "RFC whitespace in {1}")
    @MethodSource("rfcWhitespaceCases")
    void acceptsWhitespaceAtRfcSPositions(String json, String path, String expectedValues) {
        List<JsonElement> result = parser.parseExpression(path).exec(json);

        assertEquals(expectedValues, joinValues(result));
    }

    private static Stream<Arguments> executionCases() {
        return Stream.of(
                Arguments.of("{'store':{'book':[{'title':'A'},{'title':'B'}]}}",
                        "$.store.book[*].title", "A,B"),
                Arguments.of("{'title':'top','child':{'title':'leaf'}}",
                        "$..title", "top,leaf"),
                Arguments.of("{'title':'top','child':{'name':'leaf'}}",
                        "$..['title','name']", "top,leaf"),
                Arguments.of("['a','b','c','d','e','f','g']",
                        "$[0:2,5]", "a,b,f"),
                Arguments.of("['a','b','c']", "$[2,0:2,2,*]", "c,a,b,c,a,b,c"),
                Arguments.of("{'a':'A','b':'B'}", "$['b','a','b']", "B,A,B"),
                Arguments.of("{'true':1,'false':2,'null':3}", "$.true", "1"),
                Arguments.of("{'true':1,'false':2,'null':3}", "$.false", "2"),
                Arguments.of("{'true':1,'false':2,'null':3}", "$.null", "3"),
                Arguments.of("['a','b']", "$[*,*]", "a,b,a,b"));
    }

    private static Stream<Arguments> rfcWhitespaceCases() {
        return Stream.of(
                Arguments.of("['a','b','c']", "$ \t[ \n0 \r,\t 2\n ]", "a,c"),
                Arguments.of("['a','b','c','d','e']", "$ [ 1 \t: \n5 \r: 2 ]", "b,d"),
                Arguments.of("{'a':'A','b':'B'}", "$ [ 'a' \t, \n'b' ]", "A,B"),
                Arguments.of("[[1,2],[3,4]]", "$ \n..[ 0 ][0]", "1"),
                Arguments.of(
                        "[{'name':'x','blocked':false},{'name':'y','blocked':false}]",
                        "$ [ ? \t! \n( \r@ .blocked \t== \ntrue \r) \t&&\n"
                                + " match( \t@ .name \r, \n'x' \t) ] .name",
                        "x"),
                Arguments.of(
                        "[{'names':['a','b']},{'names':['a']}]",
                        "$ [ ? length( value( @ .names ) ) == 2 ] .names [ 0 ]",
                        "a"));
    }

    private static Stream<Arguments> quotedPropertyCases() {
        return Stream.of(
                Arguments.of("$['it\\'s']", "it's"),
                Arguments.of("$[\"double\\\"quote\"]", "double\"quote"),
                Arguments.of("$['\\b']", "\b"),
                Arguments.of("$['\\f']", "\f"),
                Arguments.of("$['\\n']", "\n"),
                Arguments.of("$['\\r']", "\r"),
                Arguments.of("$['\\t']", "\t"),
                Arguments.of("$['\\/']", "/"),
                Arguments.of("$['\\\\']", "\\"),
                Arguments.of("$['\\u0041']", "A"),
                Arguments.of("$['\\uD83D\\uDE00']", "😀"),
                Arguments.of("$['\\ud83d\\ude00']", "😀"));
    }

    private static Stream<Arguments> invalidExpressions() {
        return Stream.of(
                Arguments.of((String) null),
                Arguments.of(""),
                Arguments.of(" $.a"),
                Arguments.of("$. name"),
                Arguments.of("$.. name"),
                Arguments.of("$.. *"),
                Arguments.of("$.. [0]"),
                Arguments.of("$.[0]"),
                Arguments.of("$.[*]"),
                Arguments.of("$.[?@]"),
                Arguments.of("$.[ 'a' ]"),
                Arguments.of("$[?@. name]"),
                Arguments.of("$[?@.. name]"),
                Arguments.of("$[?length (@) == 1]"),
                Arguments.of("$[?match (@, 'x')]"),
                Arguments.of("$[?@ [ 'a' ] == 1]"),
                Arguments.of("$.1name"),
                Arguments.of("$.first-name"),
                Arguments.of("$.\u007F"),
                Arguments.of("$.\uD800"),
                Arguments.of("$.\uDC00"),
                Arguments.of("$.a[]"),
                Arguments.of("$['\\x']"),
                Arguments.of("$['\\U0041']"),
                Arguments.of("$[\"\\'\"]"),
                Arguments.of("$['\\\"']"),
                Arguments.of("$['\\uD800']"),
                Arguments.of("$['\\uDC00']"),
                Arguments.of("$['\\uD800\\u0041']"),
                Arguments.of("$['raw\nline']"),
                Arguments.of("$['\uD800']"),
                Arguments.of("$[9007199254740992]"),
                Arguments.of("$[-9007199254740992]"),
                Arguments.of("$[01]"),
                Arguments.of("$[-0]"));
    }

    private static Stream<String> rfcMemberNames() {
        return Stream.of(
                "ключ2",
                "café",
                "日本語",
                "\u0080",
                "\uD7FF",
                "\uE000",
                new String(Character.toChars(0x10000)),
                new String(Character.toChars(0x10FFFF)));
    }

    private static String joinValues(List<JsonElement> values) {
        StringBuilder result = new StringBuilder();
        for (JsonElement value : values) {
            if (result.length() > 0) {
                result.append(',');
            }
            result.append(value.getAsString());
        }
        return result.toString();
    }
}
