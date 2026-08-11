package net.sinistersky.j2ee.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.sinistersky.j2ee.support.iterators.PeekableIterator;
import net.sinistersky.j2ee.support.nodetypes.ArrayIndexPathNode;
import net.sinistersky.j2ee.support.nodetypes.CSVIndexPathNode;
import net.sinistersky.j2ee.support.nodetypes.PathNode;
import net.sinistersky.j2ee.support.nodetypes.RecursiveDescentPathNode;
import net.sinistersky.j2ee.support.nodetypes.SlicePathNode;
import net.sinistersky.j2ee.support.nodetypes.SelectorListPathNode;
import net.sinistersky.j2ee.support.nodetypes.WildcardPathNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class JsonPathTest {

    private final AntlrParser parser = new AntlrParser();

    @ParameterizedTest(name = "{0} has {1} nodes")
    @CsvSource(value = {
            "$|0",
            "$.aasas['.asd adsf adsf .asd asdf. asdf'].b.s.bb.c[1][2]['asas[2].hg'][0]|10",
            "$.qwe.rty|2",
            "$.c['asas[2].hg']['0']['1\\'s\\\\ds']|4",
            "$.s['1'][0]['d']|4",
            "$['1']|1",
            "$['1'][ 0]['d'].s.a[56 ].d['f']|8",
            "$.[0].[0]|2",
            "$.a['b']['c']|3",
            "$.c..v[1].d|5"
    }, delimiter = '|')
    void parsesExpressions(String path, int expectedNodeCount) {
        assertEquals(expectedNodeCount, parser.parseExpression(path).getNodes().size());
    }

    @ParameterizedTest(name = "node {1} of {0} is {2}")
    @CsvSource(value = {
            "$['1'][0]['d'].s.a[56].d['f']|4|\"a\"",
            "$['1'][0]['d'].s.a[56].d['f']|5|56",
            "$['1'][0]['d'].s.a[56].d['f']|6|\"d\""
    }, delimiter = '|')
    void rendersParsedNodes(String path, int nodeIndex, String expected) {
        assertEquals(expected, parser.parseExpression(path).getNodes().get(nodeIndex).toString());
    }

    @ParameterizedTest(name = "executes {1}")
    @MethodSource("executionCases")
    void executesExpressions(String json, String path, String expectedValues) {
        List<JsonElement> result = parser.parseExpression(path).exec(json);

        assertEquals(expectedValues, joinValues(result));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{'c':[{'v':5},{'v':51},{'v':52},{'v':{'d':777}}]}"})
    void recursiveDescentNodeFiltersDescendants(String json) {
        PeekableIterator<JsonElement> iterator =
                new RecursiveDescentPathNode().filter(JsonParser.parseString(json));
        List<JsonElement> result = drain(iterator);

        assertEquals(11, result.size());
        assertEquals(JsonParser.parseString(json), result.get(0));
        assertEquals(5, result.get(3).getAsInt());
        assertEquals(777, result.get(10).getAsInt());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{'a':'b','c':'d','e':{},'f':{'g':'h'},'i':{'j':'k','l':'m'},"
                    + "'n':{'o':'p','q':{'r':'s','t':['y',{'v':'w','x':{'y':'z'}}]},'aa':'ab'}}"
    })
    void recursiveDescentIteratorKeepsTraversalOrder(String json) {
        PeekableIterator<JsonElement> iterator =
                new RecursiveDescentPathNode().filter(JsonParser.parseString(json));

        assertEquals(JsonParser.parseString(json), iterator.next());
        assertEquals("b", iterator.next().getAsString());
        assertEquals("d", iterator.next().getAsString());
        assertTrue(iterator.next().getAsJsonObject().entrySet().isEmpty());
        assertEquals("h", iterator.next().getAsJsonObject().get("g").getAsString());
        assertEquals("h", iterator.next().getAsString());
        assertEquals("m", iterator.next().getAsJsonObject().get("l").getAsString());
        assertEquals("k", iterator.next().getAsString());
        assertEquals("m", iterator.next().getAsString());
        assertEquals("p", iterator.next().getAsJsonObject().get("o").getAsString());
        assertEquals("p", iterator.next().getAsString());
        assertEquals(9, drain(iterator).size());
    }

    @ParameterizedTest(name = "{0} creates {1}")
    @MethodSource("selectorCases")
    void parsesBracketSelectors(String path, Class<?> expectedType, int expectedIndexCount) {
        PathNode node = parser.parseExpression(path).getNodes().get(0);

        assertInstanceOf(expectedType, node);
        if (expectedIndexCount >= 0) {
            assertEquals(expectedIndexCount, ((CSVIndexPathNode) node).getIndexes().size());
        }
    }

    @ParameterizedTest(name = "rejects {0}")
    @ValueSource(strings = {
            "$.a[]",
            "$.a[   ]",
            "$.a[   ].d",
            "$[0,]",
            "$[0,1,]",
            "$[:20a]",
            "$[0:2,]"
    })
    void rejectsInvalidSelectors(String path) {
        assertThrows(JsonPathException.class, () -> parser.parseExpression(path));
    }

    private static Stream<Arguments> executionCases() {
        return Stream.of(
                Arguments.of("{'root':{'value':42}}", "$", "{\"root\":{\"value\":42}}"),
                Arguments.of("{'корінь':{'ключ':'значення'}}",
                        "$.корінь.ключ", "значення"),
                Arguments.of("{'title':'top','child':{'title':'leaf'}}",
                        "$..title", "top,leaf"),
                Arguments.of("{'title':'top','child':{'name':'leaf'}}",
                        "$..['title','name']", "top,leaf"),
                Arguments.of("[[1,2],[3,4]]", "$..[0]", "[1,2],1,3"),
                Arguments.of("[{'a':1},2]", "$..[*]", "{\"a\":1},2,1"),
                Arguments.of("['a','b','c','d','e','f']", "$[0:2,5]", "a,b,f"),
                Arguments.of("[5,2,3,4]", "$[*]", "5,2,3,4"),
                Arguments.of("{'c':{'a':'d','c':'e'}}", "$.c[ * ]", "d,e"),
                Arguments.of("{'c':[{'v':5},{'v':51},{'v':52},{'v':'lold'}]}",
                        "$.c[*].v", "5,51,52,lold"),
                Arguments.of("{'c':[{'v':5},{'v':51},{'v':52},{'v':{'d':777}}]}",
                        "$.c[*].v.d", "777"),
                Arguments.of("{'c':[{'v':5},{'v':{'d':{'x':'lol','c':2}}},{'v':52},"
                                + "{'v':{'d':[1,3,4]}}]}",
                        "$.c[*].v.d[*]", "lol,2,1,3,4"),
                Arguments.of("{'c':[{'v':5},{'v':51},{'v':52},{'c':{'v':777}}],'v':1}",
                        "$.c..v", "5,51,52,777"),
                Arguments.of("{'c':[{'v':5},{'v':51},{'v':52},{'v':{'d':777}}]}",
                        "$..v", "5,51,52,{\"d\":777}"));
    }

    private static Stream<Arguments> selectorCases() {
        return Stream.of(
                Arguments.of("$[*]", WildcardPathNode.class, -1),
                Arguments.of("$[ * ]", WildcardPathNode.class, -1),
                Arguments.of("$[0]", ArrayIndexPathNode.class, -1),
                Arguments.of("$[0 ]", ArrayIndexPathNode.class, -1),
                Arguments.of("$[0,1]", CSVIndexPathNode.class, 2),
                Arguments.of("$[0, 1 ]", CSVIndexPathNode.class, 2),
                Arguments.of("$[0,1,2,3,4,5]", CSVIndexPathNode.class, 6),
                Arguments.of("$[0:2,5]", SelectorListPathNode.class, -1),
                Arguments.of("$['a','b']", SelectorListPathNode.class, -1),
                Arguments.of("$[*,*]", SelectorListPathNode.class, -1),
                Arguments.of("$[:22]", SlicePathNode.class, -1),
                Arguments.of("$[20:13:5]", SlicePathNode.class, -1),
                Arguments.of("$[ : : 4 ]", SlicePathNode.class, -1));
    }

    private static List<JsonElement> drain(PeekableIterator<JsonElement> iterator) {
        List<JsonElement> result = new ArrayList<>();
        while (iterator.hasNext()) {
            result.add(iterator.next());
        }
        return result;
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
