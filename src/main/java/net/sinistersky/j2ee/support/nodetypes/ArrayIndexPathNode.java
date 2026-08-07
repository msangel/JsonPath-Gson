package net.sinistersky.j2ee.support.nodetypes;

import net.sinistersky.j2ee.support.iterators.OneItemIterator;
import net.sinistersky.j2ee.support.iterators.PeekableIterator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;


public class ArrayIndexPathNode implements PathNode{

    private final int index;

    public ArrayIndexPathNode(int index) {
        this.index = index;
    }

    public PeekableIterator<JsonElement> filter(JsonElement parent) {
        if(parent.isJsonArray()){
            JsonArray parentArr = parent.getAsJsonArray();
            int size = parentArr.size();
            if(index>=0 && index<size){
                JsonElement element = parentArr.get(index);
                return new OneItemIterator<>(element);
            } else if(index<0 && Math.abs(index)<=size){
                JsonElement element = parentArr.get(size+index);// so [0..size)
                return new OneItemIterator<>(element);
            }
        }
        return EMPTY_ITERATOR;
    }

    @Override
    public String toString() {
        return ""+index;
    }
}