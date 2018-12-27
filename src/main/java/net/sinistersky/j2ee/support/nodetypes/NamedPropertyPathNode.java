package net.sinistersky.j2ee.support.nodetypes;

import net.sinistersky.j2ee.support.iterators.OneItemIterator;
import net.sinistersky.j2ee.support.iterators.PeekableIterator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class NamedPropertyPathNode implements PathNode{
	private String name;

	public NamedPropertyPathNode(String name) {
		this.name = name;
	}

	public PeekableIterator<JsonElement> filter(JsonElement parent) {
		if(parent.isJsonObject()){
			JsonObject parentObj = parent.getAsJsonObject();
			if(parentObj.has(name)){
				JsonElement element = parentObj.get(name);
				return new OneItemIterator<JsonElement>(element);
			}
		}
		return EMPTY_ITERATOR;
	}
	
	@Override
	public String toString() {
		return "\""+name+"\"";
	}
}