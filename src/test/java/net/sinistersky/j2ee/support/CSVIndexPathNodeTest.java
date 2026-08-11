package net.sinistersky.j2ee.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;

import com.google.gson.JsonElement;
import net.sinistersky.j2ee.support.nodetypes.CSVIndexPathNode;
import net.sinistersky.j2ee.support.nodetypes.PathNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CSVIndexPathNodeTest {

    @ParameterizedTest(name = "{0} contains {1} indexes")
    @CsvSource(value = {
            "$[1,2,3]|3|2",
            "$[1, 2,3]|3|2",
            "$[0,1,2,3,4,5]|6|1",
            "$[-1,0]|2|10"
    }, delimiter = '|')
    void parsesAndExecutesIndexLists(String path, int indexCount, int firstValue) {
        Expression expression = new AntlrParser().parseExpression(path);
        PathNode node = expression.getNodes().get(0);

        CSVIndexPathNode indexes = assertInstanceOf(CSVIndexPathNode.class, node);
        assertEquals(indexCount, indexes.getIndexes().size());

        List<JsonElement> result = expression.exec("[1,2,3,4,5,6,7,8,9,10]");
        assertEquals(firstValue, result.get(0).getAsInt());
    }
}
