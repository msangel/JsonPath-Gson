package net.sinistersky.j2ee.support;

import com.google.gson.JsonElement;

/** A compiled RFC 9535 filter expression. */
public interface FilterExpression {
    boolean evaluate(JsonElement current, JsonElement root);
}
