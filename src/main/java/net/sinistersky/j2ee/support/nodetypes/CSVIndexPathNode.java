package net.sinistersky.j2ee.support.nodetypes;

import java.util.Iterator;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.sinistersky.j2ee.support.iterators.PeekableIterator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

@Getter
@RequiredArgsConstructor
public class CSVIndexPathNode  implements PathNode{

    private final List<Long> indexes;

    public PeekableIterator<JsonElement> filter(JsonElement parent) {
        if(parent.isJsonArray()){
            JsonArray array = parent.getAsJsonArray();
            return new CSVIndexIterator(indexes.iterator(), array);
        } else {
            return EMPTY_ITERATOR;
        }
    }

    @RequiredArgsConstructor
    private static class CSVIndexIterator extends PeekableIterator<JsonElement>{

        private final Iterator<Long> iterator;
        private final JsonArray parent;
        private boolean nextIsTaken = false;
        private JsonElement next = null;

        public boolean hasNext() {
            if(!nextIsTaken){
                next = takeNext();
                nextIsTaken = true;
            }
            return next!=null;
        }


        public JsonElement next() {
            if(!nextIsTaken){
                next = takeNext();
            } else {
                nextIsTaken = false;
            }
            return next;
        }

        @Override
        public JsonElement peek() {
            if(!nextIsTaken){
                next = takeNext();
                nextIsTaken = true;
            }
            return next;
        }

        private JsonElement takeNext(){
            while(iterator.hasNext()){
                Long index = iterator.next();
                ArrayIndexPathNode el = new ArrayIndexPathNode(index);
                PeekableIterator<JsonElement> iter = el.filter(parent);
                // this iterator can contain none elements or only one
                if(iter.hasNext()){
                    return iter.next();
                }
            }
            return null;
        }

    }
}
