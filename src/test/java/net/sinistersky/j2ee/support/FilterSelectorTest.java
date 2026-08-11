package net.sinistersky.j2ee.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import net.sinistersky.j2ee.support.nodetypes.FilterPathNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class FilterSelectorTest {

    private final AntlrParser parser = new AntlrParser();

    @ParameterizedTest(name = "executes {1}")
    @MethodSource("filterCases")
    void executesCompiledFilterExpressions(String json, String path, String expectedValues) {
        List<JsonElement> result = parser.parseExpression(path).exec(json);

        assertEquals(expectedValues, joinValues(result));
    }

    @ParameterizedTest(name = "compiles filter in {0}")
    @ValueSource(strings = {
            "$[?@ > 1]",
            "$.items[?@.price < $.limit]",
            "$.groups[?@.items[?@.active == true]]"
    })
    void compilesFilterWhileParsingJsonPath(String path) {
        Expression expression = parser.parseExpression(path);
        FilterPathNode filter = null;
        for (net.sinistersky.j2ee.support.nodetypes.PathNode node : expression.getNodes()) {
            if (node instanceof FilterPathNode) {
                filter = (FilterPathNode) node;
                break;
            }
        }

        assertNotNull(filter);
        assertNotNull(filter.getExpression());
        assertInstanceOf(FilterExpression.class, filter.getExpression());
    }

    @ParameterizedTest(name = "rejects {0}")
    @ValueSource(strings = {
            "$[?]",
            "$[?true]",
            "$[?@.price ==]",
            "$[?@.price &&]",
            "$[?(@.price]",
            "$[?@.* == 1]",
            "$[?@.price == 1 == 1]"
    })
    void rejectsInvalidOrUnsupportedFilterExpressions(String path) {
        assertThrows(JsonPathException.class, () -> parser.parseExpression(path));
    }

    @ParameterizedTest(name = "RFC comparison {0} is {1}")
    @MethodSource("comparisonCases")
    void followsRfcComparisonSemantics(String comparison, boolean expected) {
        String json = "{'candidate':0,'obj':{'x':'y'},'arr':[2,3]}";
        List<JsonElement> result = parser.parseExpression("$[?" + comparison + "]").exec(json);

        if (expected) {
            assertFalse(result.isEmpty());
        } else {
            assertEquals(0, result.size());
        }
    }

    private static Stream<Arguments> filterCases() {
        return Stream.of(
                Arguments.of(
                        "{'items':[{'name':'cheap','price':8},{'name':'expensive','price':12},"
                                + "{'name':'free','price':0}]}",
                        "$.items[?@.price < 10].name", "cheap,free"),
                Arguments.of(
                        "{'limit':10,'items':[{'name':'cheap','price':8},"
                                + "{'name':'expensive','price':12}]}",
                        "$.items[?@.price <= $.limit].name", "cheap"),
                Arguments.of(
                        "{'items':[{'name':'first','isbn':'x'},{'name':'second'},"
                                + "{'name':'third','isbn':null}]}",
                        "$.items[?@.isbn].name", "first,third"),
                Arguments.of(
                        "{'items':[{'name':'cheap','price':8,'active':true},"
                                + "{'name':'inactive','price':2,'active':false},"
                                + "{'name':'fallback','price':20,'active':false}]}",
                        "$.items[?@.price < 10 && @.active == true || "
                                + "@.name == 'fallback'].name",
                        "cheap,fallback"),
                Arguments.of(
                        "{'items':[{'name':'cheap','price':8},{'name':'expensive','price':12}]}",
                        "$.items[?!(@.price >= 10)].name", "cheap"),
                Arguments.of(
                        "{'items':[{'id':1,'name':'a,b'},{'id':2,'name':'other'}]}",
                        "$.items[?@.name == 'a,b'].id", "1"),
                Arguments.of(
                        "{'items':[{'id':1,'value':null},{'id':2,'value':false},"
                                + "{'id':3,'value':true}]}",
                        "$.items[?@.value == null || @.value == false].id", "1,2"),
                Arguments.of("{'items':[{'id':1,'true':1},{'id':2,'true':0}]}",
                        "$.items[?@.true == 1].id", "1"),
                Arguments.of("[-0.0,1,100,101]", "$[?@ == -0 || @ == 1e2]", "-0.0,100"),
                Arguments.of("{'low':1,'high':3,'text':'x'}", "$[?@ > 1]", "3"),
                Arguments.of(
                        "{'groups':[{'name':'yes','items':[{'active':false},{'active':true}]},"
                                + "{'name':'no','items':[{'active':false}]}]}",
                        "$.groups[?@.items[?@.active == true]].name", "yes"),
                Arguments.of(
                        "{'wanted':['rfc','9535'],'items':[{'name':'yes','tags':['rfc','9535']},"
                                + "{'name':'no','tags':['jsonpath']} ]}",
                        "$.items[?@.tags == $.wanted].name", "yes"),
                Arguments.of(
                        "{'items':[{'name':'one'},{'name':'two','value':2}]}",
                        "$.items[?@.missing == $.absent].name", "one,two"),
                Arguments.of(
                        "{'items':[{'name':'one'},{'name':'two','value':2}]}",
                        "$.items[?@.missing <= $.absent].name", "one,two"),
                Arguments.of(
                        "{'same':[2,3],'different':[2,4],'expected':[2,3]}",
                        "$[?@ <= $.expected]", "[2,3],[2,3]"),
                Arguments.of("[null,false,true]", "$[?@ <= @]", "null,false,true"),
                Arguments.of(
                        "{'items':[{'name':'cheap','price':8},{'name':'expensive','price':12},"
                                + "{'name':'free','price':0}]}",
                        "$.items[?@.price < 10,0].name", "cheap,free,cheap"),
                Arguments.of(
                        "{'items':[{'name':'yes','active':true},{'name':'no','active':false}]}",
                        "$..[?@.active == true].name", "yes"));
    }

    private static Stream<Arguments> comparisonCases() {
        return Stream.of(
                Arguments.of("$.absent1 == $.absent2", true),
                Arguments.of("$.absent1 <= $.absent2", true),
                Arguments.of("$.absent == 'g'", false),
                Arguments.of("$.absent1 != $.absent2", false),
                Arguments.of("$.absent != 'g'", true),
                Arguments.of("1 <= 2", true),
                Arguments.of("1 > 2", false),
                Arguments.of("13 == '13'", false),
                Arguments.of("'a' <= 'b'", true),
                Arguments.of("$.obj == $.arr", false),
                Arguments.of("$.obj == $.obj", true),
                Arguments.of("$.obj <= $.obj", true),
                Arguments.of("$.arr <= $.arr", true),
                Arguments.of("1 <= $.arr", false),
                Arguments.of("true <= true", true),
                Arguments.of("null >= null", true));
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
