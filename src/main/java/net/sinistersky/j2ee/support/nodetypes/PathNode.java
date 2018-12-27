package net.sinistersky.j2ee.support.nodetypes;

import java.util.ArrayList;

import net.sinistersky.j2ee.support.iterators.ArrayListPeekableIterator;
import net.sinistersky.j2ee.support.iterators.PeekableIterator;

import com.google.gson.JsonElement;

public abstract interface PathNode{
	final PeekableIterator<JsonElement> EMPTY_ITERATOR = new ArrayListPeekableIterator<JsonElement>(new ArrayList<JsonElement>());
	abstract PeekableIterator<JsonElement> filter(JsonElement parent);
}