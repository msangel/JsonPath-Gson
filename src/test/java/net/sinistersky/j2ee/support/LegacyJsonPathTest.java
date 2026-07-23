package net.sinistersky.j2ee.support;

import com.google.gson.JsonElement;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LegacyJsonPathTest {

    @Test
    void parsesDottedBracketedAndRecursivePathNodes() {
        JsonPath parser = new JsonPath();
        JsonPath.Expression bracketedExpression = parser.parseExpression("$.store['book'][0]");

        List<JsonPath.PathNode> bracketedNodes = bracketedExpression.getNodes();

        assertEquals(3, bracketedNodes.size());
        assertInstanceOf(JsonPath.NamedPropertyPathNode.class, bracketedNodes.get(0));
        assertInstanceOf(JsonPath.NamedPropertyPathNode.class, bracketedNodes.get(1));
        assertInstanceOf(JsonPath.ArrayIndexPathNode.class, bracketedNodes.get(2));

        JsonPath.Expression recursiveExpression = parser.parseExpression("$.store.book..title");
        List<JsonPath.PathNode> recursiveNodes = recursiveExpression.getNodes();

        assertEquals(4, recursiveNodes.size());
        assertInstanceOf(JsonPath.NamedPropertyPathNode.class, recursiveNodes.get(0));
        assertInstanceOf(JsonPath.NamedPropertyPathNode.class, recursiveNodes.get(1));
        assertInstanceOf(JsonPath.RecursiveDescentPathNode.class, recursiveNodes.get(2));
        assertInstanceOf(JsonPath.NamedPropertyPathNode.class, recursiveNodes.get(3));
    }

    @Test
    void executesPropertyIndexAndWildcardSelections() {
        String json = "{'store':{'book':[{'title':'A','price':8},{'title':'B','price':12}]}}";
        JsonPath.Expression expression = new JsonPath()
                .parseExpression("$.store.book[*].title");

        List<JsonElement> titles = expression.exec(json);

        assertEquals(2, titles.size());
        assertEquals("A", titles.get(0).getAsString());
        assertEquals("B", titles.get(1).getAsString());
    }

    @Test
    void wildcardReturnsObjectValuesInInsertionOrder() {
        String json = "{'meta':{'first':1,'second':2}}";
        JsonPath.Expression expression = new JsonPath().parseExpression("$.meta[*]");

        List<JsonElement> values = expression.exec(json);

        assertEquals(2, values.size());
        assertEquals(1, values.get(0).getAsInt());
        assertEquals(2, values.get(1).getAsInt());
    }

    @Test
    void recursiveDescentFindsNestedProperties() {
        String json = "{'root':{'title':'top','children':[{'title':'leaf'},{'name':'skip'}]}}";
        JsonPath.Expression expression = new JsonPath().parseExpression("$..title");

        List<JsonElement> titles = expression.exec(json);

        assertEquals(2, titles.size());
        assertEquals("top", titles.get(0).getAsString());
        assertEquals("leaf", titles.get(1).getAsString());
    }

    @Test
    void quotedPropertyNamesCanContainEscapedQuotesAndBackslashes() {
        String json = "{'a':{'it\\'s\\\\ok':42}}";
        JsonPath.Expression expression = new JsonPath()
                .parseExpression("$.a['it\\'s\\\\ok']");

        List<JsonElement> values = expression.exec(json);

        assertEquals(1, values.size());
        assertEquals(42, values.get(0).getAsInt());
    }

    @Test
    void rejectsInvalidExpressions() {
        JsonPath parser = new JsonPath();

        assertThrows(JsonPath.JsonPathException.class, () -> parser.parseExpression(null));
        assertThrows(JsonPath.JsonPathException.class, () -> parser.parseExpression(""));
        assertThrows(JsonPath.JsonPathException.class, () -> parser.parseExpression(" $.a"));
        assertThrows(JsonPath.JsonPathException.class, () -> parser.parseExpression("a.b"));
        assertThrows(JsonPath.JsonPathException.class, () -> parser.parseExpression("$"));
        assertThrows(JsonPath.JsonPathException.class, () -> parser.parseExpression("$.a b"));
        assertThrows(JsonPath.JsonPathException.class, () -> parser.parseExpression("$.a['b' 0]"));
        assertThrows(JsonPath.JsonPathException.class, () -> parser.parseExpression("$.a['b'"));
    }
}
