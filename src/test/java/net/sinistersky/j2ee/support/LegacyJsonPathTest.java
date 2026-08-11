package net.sinistersky.j2ee.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import net.sinistersky.j2ee.support.nodetypes.ArrayIndexPathNode;
import net.sinistersky.j2ee.support.nodetypes.NamedPropertyPathNode;
import net.sinistersky.j2ee.support.nodetypes.PathNode;
import net.sinistersky.j2ee.support.nodetypes.RecursiveDescentPathNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class LegacyJsonPathTest {

    @ParameterizedTest(name = "parses {0}")
    @MethodSource("pathNodeCases")
    void parsesPathNodes(String path, Class<?>[] expectedTypes) {
        List<PathNode> nodes = new JsonPath().parseExpression(path).getNodes();

        assertEquals(expectedTypes.length, nodes.size());
        for (int index = 0; index < expectedTypes.length; index++) {
            assertInstanceOf(expectedTypes[index], nodes.get(index));
        }
    }

    @ParameterizedTest(name = "executes {1}")
    @MethodSource("executionCases")
    void executesSelections(String json, String path, String expectedValues) {
        List<JsonElement> values = new JsonPath().parseExpression(path).exec(json);

        assertEquals(expectedValues, joinValues(values));
    }

    @ParameterizedTest(name = "rejects [{0}]")
    @MethodSource("invalidExpressions")
    void rejectsInvalidExpressions(String path) {
        assertThrows(JsonPathException.class, () -> new JsonPath().parseExpression(path));
    }

    private static Stream<Arguments> pathNodeCases() {
        return Stream.of(
                Arguments.of("$", new Class<?>[0]),
                Arguments.of("$.store['book'][0]", new Class<?>[] {
                        NamedPropertyPathNode.class,
                        NamedPropertyPathNode.class,
                        ArrayIndexPathNode.class
                }),
                Arguments.of("$.store.book..title", new Class<?>[] {
                        NamedPropertyPathNode.class,
                        NamedPropertyPathNode.class,
                        RecursiveDescentPathNode.class,
                        NamedPropertyPathNode.class
                }));
    }

    private static Stream<Arguments> executionCases() {
        return Stream.of(
                Arguments.of("{'root':[1,2,3]}", "$", "{\"root\":[1,2,3]}"),
                Arguments.of("{'store':{'book':[{'title':'A','price':8},{'title':'B','price':12}]}}",
                        "$.store.book[*].title", "A,B"),
                Arguments.of("{'meta':{'first':1,'second':2}}", "$.meta[*]", "1,2"),
                Arguments.of("{'root':{'title':'top','children':[{'title':'leaf'},{'name':'skip'}]}}",
                        "$..title", "top,leaf"),
                Arguments.of("{'a':{'it\\'s\\\\ok':42}}", "$.a['it\\'s\\\\ok']", "42"));
    }

    private static Stream<Arguments> invalidExpressions() {
        return Stream.of(
                Arguments.of((String) null),
                Arguments.of(""),
                Arguments.of(" $.a"),
                Arguments.of("a.b"),
                Arguments.of("$.a b"),
                Arguments.of("$.a['b' 0]"),
                Arguments.of("$.a['b'"));
    }

    private static String joinValues(List<JsonElement> values) {
        StringBuilder result = new StringBuilder();
        for (JsonElement value : values) {
            if (result.length() > 0) {
                result.append(',');
            }
            if (value.isJsonPrimitive()) {
                result.append(value.getAsString());
            } else {
                result.append(value);
            }
        }
        return result.toString();
    }
}
