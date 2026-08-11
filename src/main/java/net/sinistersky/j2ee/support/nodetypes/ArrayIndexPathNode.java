package net.sinistersky.j2ee.support.nodetypes;

import lombok.RequiredArgsConstructor;
import net.sinistersky.j2ee.support.iterators.OneItemIterator;
import net.sinistersky.j2ee.support.iterators.PeekableIterator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;


@RequiredArgsConstructor
public class ArrayIndexPathNode implements PathNode{

    private final long index;

    public PeekableIterator<JsonElement> filter(JsonElement parent) {
        if(parent.isJsonArray()){
            JsonArray parentArr = parent.getAsJsonArray();
            long size = parentArr.size();
            if(index>=0 && index<size){
                JsonElement element = parentArr.get((int) index);
                return new OneItemIterator<>(element);
            } else if(index<0 && size+index>=0){
                JsonElement element = parentArr.get((int) (size+index));// so [0..size)
                return new OneItemIterator<>(element);
            }
        }
        return EMPTY_ITERATOR;
    }

    @Override
    public String toString() {
        return Long.toString(index);
    }
}
