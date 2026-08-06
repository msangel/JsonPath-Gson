package net.sinistersky.j2ee.support;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.sinistersky.j2ee.support.iterators.ArrayListPeekableIterator;
import net.sinistersky.j2ee.support.iterators.ExecIterator;
import net.sinistersky.j2ee.support.iterators.PeekableIterator;
import net.sinistersky.j2ee.support.nodetypes.PathNode;

import java.util.ArrayList;
import java.util.List;

public class Expression {

    final List<PathNode> nodes;

    public Expression(List<PathNode> nodes) {
        this.nodes = nodes;
    }

    public List<PathNode> getNodes() {
        return nodes;
    }

    List<JsonElement> exec(String strJson) {
        return exec(JsonParser.parseString(strJson));
    }

    List<JsonElement> exec(JsonElement obj) {
        ArrayList<JsonElement> list = new ArrayList<>();
        list.add(obj);
        PeekableIterator<JsonElement> iterator = exec(new ArrayListPeekableIterator<>(list));
        List<JsonElement> res = new ArrayList<>();
        while (iterator.hasNext()) {
            res.add(iterator.next());
        }
        return res;
    }

    PeekableIterator<JsonElement> exec(final PeekableIterator<JsonElement> in) {
        return new ExecIterator(this, in, 0);
    }
}
