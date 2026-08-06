package net.sinistersky.j2ee.support;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.sinistersky.j2ee.support.iterators.ArrayListPeekableIterator;
import net.sinistersky.j2ee.support.iterators.ExecIterator;
import net.sinistersky.j2ee.support.iterators.PeekableIterator;

import java.util.ArrayList;
import java.util.List;

public class JsonPath {



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
		return new Parser().parseExpression(str);
	}
}
