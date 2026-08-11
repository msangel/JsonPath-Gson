package net.sinistersky.j2ee.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class FunctionExtensionTest {

    private final AntlrParser parser = new AntlrParser();

    @ParameterizedTest(name = "executes {1}")
    @MethodSource("functionCases")
    void executesStandardFunctionExtensions(String json, String path, String expectedValues) {
        List<JsonElement> result = parser.parseExpression(path).exec(json);

        assertEquals(expectedValues, joinValues(result));
    }

    @ParameterizedTest(name = "invalid I-Regexp in {0} evaluates to false")
    @ValueSource(strings = {
            "$[?match(@, '\\\\d+')]",
            "$[?match(@, '(?=abc)')]",
            "$[?match(@, '[^]')]",
            "$[?match(@, 'a**')]",
            "$[?match(@, '\\\\p{BasicLatin}') ]"
    })
    void invalidIRegexpEvaluatesToFalse(String path) {
        assertEquals(0, parser.parseExpression(path).exec("['abc','123']").size());
    }

    @ParameterizedTest(name = "rejects non-well-typed function expression {0}")
    @ValueSource(strings = {
            "$[?length(@.*) < 3]",
            "$[?count(1) == 1]",
            "$[?match(@.timezone, 'Europe/.*') == true]",
            "$[?value(@..color)]",
            "$[?length(match(@, 'x')) == 1]",
            "$[?count(value(@.*)) == 1]",
            "$[?match(@)]",
            "$[?search(@, 'x', 'y')]",
            "$[?length() == 0]",
            "$[?unknown(@)]"
    })
    void rejectsInvalidFunctionExpressions(String path) {
        assertThrows(JsonPathException.class, () -> parser.parseExpression(path));
    }

    private static Stream<Arguments> functionCases() {
        return Stream.of(
                Arguments.of(
                        "{'items':[{'id':'string','value':'ab'},"
                                + "{'id':'unicode','value':'😀x'},"
                                + "{'id':'array','value':[1,2]},"
                                + "{'id':'object','value':{'a':1,'b':2}},"
                                + "{'id':'number','value':2},{'id':'missing'}]}",
                        "$.items[?length(@.value) == 2].id",
                        "string,unicode,array,object"),
                Arguments.of("[1,2]", "$[?length('😀') == 1]", "1,2"),
                Arguments.of("[[1,2],[1,2,3],'ab']", "$[?length(@) < 3]", "[1,2],ab"),
                Arguments.of(
                        "{'items':[{'id':'two','values':[1,2]},"
                                + "{'id':'one','values':[1]},{'id':'none'}]}",
                        "$.items[?count(@.values[*]) == 2].id", "two"),
                Arguments.of("{'items':[{'id':'yes','a':1},{'id':'no'}]}",
                        "$.items[?count(@['a','a']) == 2].id", "yes"),
                Arguments.of("{'items':[{'id':'one'},{'id':'two'}]}",
                        "$.items[?count(@) == 1].id", "one,two"),
                Arguments.of(
                        "{'groups':[{'id':'one','data':{'color':'red'}},"
                                + "{'id':'many','a':{'color':'red'},'b':{'color':'blue'}},"
                                + "{'id':'none'}]}",
                        "$.groups[?value(@..color) == 'red'].id", "one"),
                Arguments.of(
                        "{'groups':[{'id':'one','data':{'color':'red'}},"
                                + "{'id':'many','a':{'color':'red'},'b':{'color':'blue'}},"
                                + "{'id':'none'}]}",
                        "$.groups[?value(@..color) == $.absent].id", "many,none"),
                Arguments.of("{'items':[{'id':'yes','names':['a','b']},"
                                + "{'id':'no','names':['a']} ]}",
                        "$.items[?length(value(@.names)) == 2].id", "yes"),
                Arguments.of(
                        "{'a':[{'b':'j'},{'b':'k'},{'b':'kilo'},{'b':'other'}]}",
                        "$.a[?match(@.b, '[jk]')].b", "j,k"),
                Arguments.of(
                        "{'a':[{'b':'j'},{'b':'k'},{'b':'kilo'},{'b':'other'}]}",
                        "$.a[?search(@.b, '[jk]')].b", "j,k,kilo"),
                Arguments.of(
                        "{'items':[{'id':'date','value':'1974-05-01'},"
                                + "{'id':'partial','value':'x1974-05-01'}]}",
                        "$.items[?match(@.value, '1974-05-..')].id", "date"),
                Arguments.of(
                        "{'items':[{'id':'literal','value':'$^'},"
                                + "{'id':'other','value':'abc'}]}",
                        "$.items[?match(@.value, '$^')].id", "literal"),
                Arguments.of(
                        "{'items':[{'id':'greek','value':'αβ'},"
                                + "{'id':'digits','value':'12'}]}",
                        "$.items[?match(@.value, '\\\\p{L}+')].id", "greek"),
                Arguments.of(
                        "{'items':[{'id':'newline','value':'a\\nb'},"
                                + "{'id':'separator','value':'a b'}]}",
                        "$.items[?match(@.value, 'a.b')].id", "separator"),
                Arguments.of("{'length':1,'count':2,'match':3,'search':4,'value':5}",
                        "$.length", "1"),
                Arguments.of("{'length':1,'count':2,'match':3,'search':4,'value':5}",
                        "$.count", "2"),
                Arguments.of("{'length':1,'count':2,'match':3,'search':4,'value':5}",
                        "$.match", "3"),
                Arguments.of("{'length':1,'count':2,'match':3,'search':4,'value':5}",
                        "$.search", "4"),
                Arguments.of("{'length':1,'count':2,'match':3,'search':4,'value':5}",
                        "$.value", "5"));
    }

    private static String joinValues(List<JsonElement> values) {
        StringBuilder result = new StringBuilder();
        for (JsonElement value : values) {
            if (result.length() > 0) {
                result.append(',');
            }
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                result.append(value.getAsString());
            } else {
                result.append(value);
            }
        }
        return result.toString();
    }
}
