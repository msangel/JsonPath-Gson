package net.sinistersky.j2ee.support.nodetypes;

import com.google.gson.JsonElement;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.sinistersky.j2ee.support.FilterExpression;
import net.sinistersky.j2ee.support.iterators.PeekableIterator;
import net.sinistersky.j2ee.support.iterators.WildcardIterator;

@Getter
@RequiredArgsConstructor
public final class FilterPathNode implements PathNode {

    private final FilterExpression expression;

    @Override
    public PeekableIterator<JsonElement> filter(JsonElement parent) {
        return filter(parent, parent);
    }

    @Override
    public PeekableIterator<JsonElement> filter(JsonElement parent, JsonElement root) {
        return new FilteringIterator(new WildcardIterator(parent), expression, root);
    }

    @Override
    public String toString() {
        return "?";
    }

    private static final class FilteringIterator extends PeekableIterator<JsonElement> {
        private final PeekableIterator<JsonElement> candidates;
        private final FilterExpression expression;
        private final JsonElement root;
        private JsonElement next;
        private boolean nextTaken;

        private FilteringIterator(PeekableIterator<JsonElement> candidates,
                FilterExpression expression, JsonElement root) {
            this.candidates = candidates;
            this.expression = expression;
            this.root = root;
        }

        @Override
        public boolean hasNext() {
            if (!nextTaken) {
                next = takeNext();
                nextTaken = true;
            }
            return next != null;
        }

        @Override
        public JsonElement next() {
            if (!nextTaken) {
                next = takeNext();
            }
            nextTaken = false;
            return next;
        }

        @Override
        public JsonElement peek() {
            if (!nextTaken) {
                next = takeNext();
                nextTaken = true;
            }
            return next;
        }

        private JsonElement takeNext() {
            while (candidates.hasNext()) {
                JsonElement candidate = candidates.next();
                if (expression.evaluate(candidate, root)) {
                    return candidate;
                }
            }
            return null;
        }
    }
}
