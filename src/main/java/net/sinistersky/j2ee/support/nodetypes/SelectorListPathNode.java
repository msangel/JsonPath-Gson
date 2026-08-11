package net.sinistersky.j2ee.support.nodetypes;

import java.util.Iterator;
import java.util.List;

import com.google.gson.JsonElement;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.sinistersky.j2ee.support.iterators.PeekableIterator;

@Getter
@RequiredArgsConstructor
public class SelectorListPathNode implements PathNode {

    private final List<PathNode> selectors;

    @Override
    public PeekableIterator<JsonElement> filter(JsonElement parent) {
        return new SelectorListIterator(selectors.iterator(), parent);
    }

    @RequiredArgsConstructor
    private static final class SelectorListIterator extends PeekableIterator<JsonElement> {
        private final Iterator<PathNode> selectors;
        private final JsonElement parent;
        private PeekableIterator<JsonElement> current = EMPTY_ITERATOR;

        @Override
        public boolean hasNext() {
            while (!current.hasNext() && selectors.hasNext()) {
                current = selectors.next().filter(parent);
            }
            return current.hasNext();
        }

        @Override
        public JsonElement next() {
            return hasNext() ? current.next() : null;
        }

        @Override
        public JsonElement peek() {
            return hasNext() ? current.peek() : null;
        }
    }
}
