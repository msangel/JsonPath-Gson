package net.sinistersky.j2ee.support.nodetypes;

import java.util.ArrayList;

import net.sinistersky.j2ee.support.iterators.ArrayListPeekableIterator;
import net.sinistersky.j2ee.support.iterators.PeekableIterator;

import com.google.gson.JsonElement;

public interface PathNode {
    PeekableIterator<JsonElement> EMPTY_ITERATOR = new ArrayListPeekableIterator<>(new ArrayList<>());
    PeekableIterator<JsonElement> filter(JsonElement parent);
}