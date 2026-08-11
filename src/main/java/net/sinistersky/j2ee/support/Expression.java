package net.sinistersky.j2ee.support;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.sinistersky.j2ee.support.iterators.ArrayListPeekableIterator;
import net.sinistersky.j2ee.support.iterators.ExecIterator;
import net.sinistersky.j2ee.support.iterators.PeekableIterator;
import net.sinistersky.j2ee.support.nodetypes.PathNode;

import java.util.ArrayList;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class Expression {

    final List<PathNode> nodes;

    public List<JsonElement> exec(String strJson) {
        return exec(JsonParser.parseString(strJson));
    }

    public List<JsonElement> exec(JsonElement obj) {
        return execFrom(obj, obj);
    }

    List<JsonElement> execFrom(JsonElement start, JsonElement root) {
        ArrayList<JsonElement> list = new ArrayList<>();
        list.add(start);
        PeekableIterator<JsonElement> iterator = new ExecIterator(
                this, new ArrayListPeekableIterator<>(list), 0, root);
        List<JsonElement> res = new ArrayList<>();
        while (iterator.hasNext()) {
            res.add(iterator.next());
        }
        return res;
    }

}
