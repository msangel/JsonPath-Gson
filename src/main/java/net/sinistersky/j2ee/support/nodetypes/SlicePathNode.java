package net.sinistersky.j2ee.support.nodetypes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import lombok.Getter;
import net.sinistersky.j2ee.support.iterators.PeekableIterator;

public class SlicePathNode implements PathNode {

    private static final class SliceIterator extends PeekableIterator<JsonElement> {
        private final JsonArray array;
        private final long end;
        private final long step;
        private long position;

        private SliceIterator(JsonArray array, Long from, Long to, long step) {
            this.array = array;
            this.step = step;

            long length = array.size();
            if (step > 0) {
                position = clamp(normalize(from == null ? 0L : from, length), 0, length);
                end = clamp(normalize(to == null ? length : to, length), 0, length);
            } else {
                position = clamp(from == null ? length - 1 : normalize(from, length),
                        -1, length - 1);
                end = clamp(to == null ? -1 : normalize(to, length), -1, length - 1);
            }
        }

        @Override
        public boolean hasNext() {
            return step > 0 ? position < end : end < position;
        }

        @Override
        public JsonElement next() {
            if (!hasNext()) {
                return null;
            }
            JsonElement result = array.get((int) position);
            position += step;
            return result;
        }

        @Override
        public JsonElement peek() {
            return hasNext() ? array.get((int) position) : null;
        }

        private static long normalize(long index, long length) {
            return index >= 0 ? index : length + index;
        }

        private static long clamp(long value, long minimum, long maximum) {
            return Math.min(Math.max(value, minimum), maximum);
        }
    }

    @Getter
    private final Long from;
    @Getter
    private final Long to;
    @Getter
    private final Long step;

    public SlicePathNode(Long from, Long to, Long step) {
        this.from = from;
        this.to = to;
        this.step = step == null ? 1L : step;
    }

    @Override
    public PeekableIterator<JsonElement> filter(JsonElement parent) {
        if (!parent.isJsonArray() || step == 0) {
            return EMPTY_ITERATOR;
        }
        return new SliceIterator(parent.getAsJsonArray(), from, to, step);
    }
}
