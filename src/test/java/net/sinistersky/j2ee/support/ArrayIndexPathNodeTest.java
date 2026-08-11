package net.sinistersky.j2ee.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.google.gson.JsonElement;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ArrayIndexPathNodeTest {

    private static final String DATA = "[1,2,3,4,5,6,7,8,9,10]";

    @ParameterizedTest(name = "{0} selects {1}")
    @CsvSource(value = {
            "$[0]|1",
            "$[9]|10",
            "$[10]|null",
            "$[-1]|10",
            "$[-9]|2",
            "$[-10]|1",
            "$[-11]|null",
            "$[9007199254740991]|null",
            "$[-9007199254740991]|null"
    }, delimiter = '|', nullValues = "null")
    void selectsArrayIndex(String path, Integer expected) {
        List<JsonElement> result = new AntlrParser().parseExpression(path).exec(DATA);

        if (expected == null) {
            assertTrue(result.isEmpty());
        } else {
            assertEquals(expected.intValue(), result.get(0).getAsInt());
        }
    }

    @ParameterizedTest(name = "rejects {0}")
    @ValueSource(strings = {
            "$[00]",
            "$[01]",
            "$[-0]",
            "$[-01]",
            "$[9007199254740992]",
            "$[-9007199254740992]"
    })
    void rejectsIndexesOutsideRfcSyntaxOrRange(String path) {
        assertThrows(JsonPathException.class, () -> new AntlrParser().parseExpression(path));
    }
}
