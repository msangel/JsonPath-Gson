package net.sinistersky.j2ee.support;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.sinistersky.j2ee.support.iterators.ArrayListPeekableIterator;
import net.sinistersky.j2ee.support.iterators.ExecIterator;
import net.sinistersky.j2ee.support.iterators.PeekableIterator;

import java.util.ArrayList;
import java.util.List;

public class JsonPath {

	public interface PathNode extends net.sinistersky.j2ee.support.nodetypes.PathNode {
	}

	public static class NamedPropertyPathNode
			extends net.sinistersky.j2ee.support.nodetypes.NamedPropertyPathNode
			implements PathNode {
		public NamedPropertyPathNode(String name) {
			super(name);
		}
	}

	public static class ArrayIndexPathNode
			extends net.sinistersky.j2ee.support.nodetypes.ArrayIndexPathNode
			implements PathNode {
		public ArrayIndexPathNode(int index) {
			super(index);
		}
	}

	public static class WildcardPathNode
			extends net.sinistersky.j2ee.support.nodetypes.WildcardPathNode
			implements PathNode {
	}

	public static class RecursiveDescentPathNode
			extends net.sinistersky.j2ee.support.nodetypes.RecursiveDescentPathNode
			implements PathNode {
	}

	public static class JsonPathException extends net.sinistersky.j2ee.support.JsonPathException {
		private static final long serialVersionUID = 1L;

		public JsonPathException(net.sinistersky.j2ee.support.JsonPathException cause) {
			super(cause.getMessage());
			initCause(cause);
		}
	}

	static public class Expression {

		final List<net.sinistersky.j2ee.support.nodetypes.PathNode> nodes;

		public Expression(List<net.sinistersky.j2ee.support.nodetypes.PathNode> nodes) {
			this.nodes = nodes;
		}

		@SuppressWarnings("unchecked")
		public <T extends net.sinistersky.j2ee.support.nodetypes.PathNode> List<T> getNodes() {
			return (List<T>) nodes;
		}

		List<JsonElement> exec(String strJson) {
			return exec(new JsonParser().parse(strJson));
		}

		List<JsonElement> exec(JsonElement obj) {
			ArrayList<JsonElement> list = new ArrayList<>();
			list.add(obj);
			int filterPosition = 0;
			PeekableIterator<JsonElement> iterator = exec(new ArrayListPeekableIterator<>(list), filterPosition);
			List<JsonElement> res = new ArrayList<>();
			while (iterator.hasNext()) {
				res.add(iterator.next());
			}
			return res;
		}

		PeekableIterator<JsonElement> exec(final PeekableIterator<JsonElement> in, final int filterPosition) {
			return new ExecIterator(this, in, filterPosition);
		}
	}

	Expression parseExpression(String str) throws JsonPathException {
		try {
			return new Parser().parseExpression(str);
		} catch (net.sinistersky.j2ee.support.JsonPathException e) {
			throw new JsonPathException(e);
		}
	}
}
