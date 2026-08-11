package net.sinistersky.j2ee.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import net.sinistersky.j2ee.support.nodetypes.SlicePathNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class SlicePathNodeTest {

    private static final String ARRAY = "[1,2,3,4,5,6,7,8,9,10]";

    @ParameterizedTest(name = "{0} returns {1} elements")
    @MethodSource("sliceCases")
    void executesSlices(String path, int expectedCount, Integer expectedFirst) {
        List<JsonElement> result = new AntlrParser().parseExpression(path).exec(ARRAY);

        assertEquals(expectedCount, result.size());
        if (expectedFirst != null) {
            assertEquals(expectedFirst.intValue(), result.get(0).getAsInt());
        }
    }

    @ParameterizedTest(name = "parses {0}")
    @MethodSource("sliceNodeCases")
    void parsesSliceBounds(String path, Long from, Long to, long step) {
        SlicePathNode slice = assertInstanceOf(SlicePathNode.class,
                new AntlrParser().parseExpression(path).getNodes().get(0));

        assertEquals(from, slice.getFrom());
        assertEquals(to, slice.getTo());
        assertEquals(step, slice.getStep().longValue());
    }

    @ParameterizedTest(name = "rejects {0}")
    @ValueSource(strings = {
            "$[:20a]",
            "$[0,]",
            "$[0,1,]",
            "$[01:2]",
            "$[-0:2]",
            "$[0:9007199254740992]",
            "$[0:2:-9007199254740992]"
    })
    void rejectsInvalidSlices(String path) {
        assertThrows(JsonPathException.class, () -> new AntlrParser().parseExpression(path));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("wildcardSliceCases")
    void slicesSelectOnlyArrayElements(String json, String path, String expectedValues) {
        List<JsonElement> result = new AntlrParser().parseExpression(path).exec(json);

        assertEquals(expectedValues, joinValues(result));
    }

    private static Stream<Arguments> sliceCases() {
        return Stream.of(
                slice("$[0:9]", 9, 1),
                slice("$[:9]", 9, 1),
                slice("$[0:-1]", 9, 1),
                slice("$[:-1]", 9, 1),
                slice("$[1:]", 9, 2),
                slice("$[-9:]", 9, 2),
                slice("$[::]", 10, 1),
                slice("$[2:4]", 2, 3),
                slice("$[2:-6]", 2, 3),
                slice("$[-8:-6]", 2, 3),
                slice("$[-8:4]", 2, 3),
                slice("$[-4:-2]", 2, 7),
                slice("$[4:2:-1]", 2, 5),
                slice("$[9::-1]", 10, 10),
                slice("$[-1::-1]", 10, 10),
                slice("$[-1:-11:-1]", 10, 10),
                slice("$[:6:2]", 3, 1),
                slice("$[1:9:3]", 3, 2),
                slice("$[:5:-1]", 4, 10),
                slice("$[:-4:-1]", 3, 10),
                slice("$[3::-1]", 4, 4),
                slice("$[-8::-1]", 3, 3),
                slice("$[3:7:2]", 2, 4),
                slice("$[10:]", 0, null),
                slice("$[:-10]", 0, null),
                slice("$[2:1]", 0, null),
                slice("$[-1:-2]", 0, null),
                slice("$[-11::-1]", 0, null),
                slice("$[:9:-1]", 0, null),
                slice("$[1:2:-1]", 0, null),
                slice("$[-2:-1:-1]", 0, null),
                slice("$[0:13]", 10, 1),
                slice("$[:13]", 10, 1),
                slice("$[:14]", 10, 1),
                slice("$[-13:-1]", 9, 1),
                slice("$[:-15]", 0, null),
                slice("$[100:]", 0, null),
                slice("$[-99:]", 10, 1),
                slice("$[20:40]", 0, null),
                slice("$[20:-60]", 0, null),
                slice("$[-80:-60]", 0, null),
                slice("$[-80:40]", 10, 1),
                slice("$[-40:-20]", 0, null),
                slice("$[40:20:-10]", 0, null),
                slice("$[90::-10]", 1, 10),
                slice("$[-10::-10]", 1, 1),
                slice("$[-10:-110:-10]", 1, 1),
                slice("$[:60:20]", 1, 1),
                slice("$[1:90:30]", 1, 2),
                slice("$[:50:-10]", 0, null),
                slice("$[:-40:-10]", 1, 10),
                slice("$[30::-10]", 1, 10),
                slice("$[-80::-10]", 0, null),
                slice("$[30:70:20]", 0, null),
                slice("$[0:10:0]", 0, null),
                slice("$[-9007199254740991:9007199254740991:9007199254740991]",
                        1, 1));
    }

    private static Stream<Arguments> sliceNodeCases() {
        return Stream.of(
                Arguments.of("$[:22]", null, 22L, 1L),
                Arguments.of("$[ : 21 ]", null, 21L, 1L),
                Arguments.of("$[20:]", 20L, null, 1L),
                Arguments.of("$[20 : ]", 20L, null, 1L),
                Arguments.of("$[20:13:5]", 20L, 13L, 5L),
                Arguments.of("$[20 : 13 : 5]", 20L, 13L, 5L),
                Arguments.of("$[-20:13:2]", -20L, 13L, 2L),
                Arguments.of("$[20:-13:3]", 20L, -13L, 3L),
                Arguments.of("$[-20:-13]", -20L, -13L, 1L),
                Arguments.of("$[-1:]", -1L, null, 1L),
                Arguments.of("$[::2]", null, null, 2L),
                Arguments.of("$[::]", null, null, 1L),
                Arguments.of("$[ : : 4 ]", null, null, 4L),
                Arguments.of("$[-9007199254740991:9007199254740991:0]",
                        -9007199254740991L, 9007199254740991L, 0L));
    }

    private static Stream<Arguments> wildcardSliceCases() {
        return Stream.of(
                Arguments.of("{'c':[{'v':5},{'v':51},{'v':52},{'c':{'v':777}}]}",
                        "$.c[::].v", "5,51,52"),
                Arguments.of("{'c':{'d':{'v':5},'r':{'v':51}}}",
                        "$.c[:].v", ""),
                Arguments.of("{'c':{'d':{'v':5},'r':{'v':51}}}",
                        "$.c[ : : ].v", ""));
    }

    private static Arguments slice(String path, int count, Integer first) {
        return Arguments.of(path, count, first);
    }

    private static String joinValues(List<JsonElement> values) {
        StringBuilder result = new StringBuilder();
        for (JsonElement value : values) {
            if (result.length() > 0) {
                result.append(',');
            }
            result.append(value.getAsInt());
        }
        return result.toString();
    }
}
