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

		public JsonPathException(String message) {
			super(message);
		}

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
			ArrayList<JsonElement> list = new ArrayList<JsonElement>();
			list.add(obj);
			int filterPosition = 0;
			PeekableIterator<JsonElement> iterator = exec(new ArrayListPeekableIterator<JsonElement>(list), filterPosition);
			List<JsonElement> res = new ArrayList<JsonElement>();
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

	public static void assertEquals(Object a, Object b) {
		if ((a == null && b != null) || !a.equals(b)) {
			throw new RuntimeException("<" + a + "> is not same as <" + b + ">");
		}
	}

	public static void assertTrue(Boolean a) {
		if (a == null) {
			throw new RuntimeException("null is not same as True");
		}
		if (!a.booleanValue()) {
			throw new RuntimeException("<" + a + "> is not True");
		}
	}

	public static String getStackOffset() {
		StringBuilder sb = new StringBuilder();
		for (int i = 6; i < Thread.currentThread().getStackTrace().length; i++) {
			sb.append("\t");
		}
		return sb.toString();
	}
}
