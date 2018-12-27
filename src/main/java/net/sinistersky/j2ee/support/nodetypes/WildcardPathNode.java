package net.sinistersky.j2ee.support.nodetypes;

import net.sinistersky.j2ee.support.iterators.PeekableIterator;
import net.sinistersky.j2ee.support.iterators.WildcardIterator;

import com.google.gson.JsonElement;

public class WildcardPathNode implements PathNode{
	public PeekableIterator<JsonElement> filter(JsonElement parent) {
		return new WildcardIterator(parent); 
	}
	
	@Override
	public String toString() {
		return "*";
	}
}