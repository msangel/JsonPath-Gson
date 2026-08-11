package net.sinistersky.j2ee.support;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

class FilterExpressions {

    interface ValueExpression {
        FilterValue evaluate(JsonElement current, JsonElement root);
    }

    static final class FilterValue {
        private static final FilterValue NOTHING = new FilterValue(null, false);

        private final JsonElement value;
        private final boolean present;

        private FilterValue(JsonElement value, boolean present) {
            this.value = value;
            this.present = present;
        }

        private static FilterValue of(JsonElement value) {
            return new FilterValue(value, true);
        }
    }

    private FilterExpressions() {
    }

    static FilterExpression and(final List<FilterExpression> expressions) {
        return (current, root) -> {
            for (FilterExpression expression : expressions) {
                if (!expression.evaluate(current, root)) {
                    return false;
                }
            }
            return true;
        };
    }

    static FilterExpression or(final List<FilterExpression> expressions) {
        return (current, root) -> {
            for (FilterExpression expression : expressions) {
                if (expression.evaluate(current, root)) {
                    return true;
                }
            }
            return false;
        };
    }

    static FilterExpression not(final FilterExpression expression) {
        return (current, root) -> !expression.evaluate(current, root);
    }

    static FilterExpression queryTest(final Expression query, final boolean relative) {
        return (current, root) -> {
            JsonElement start = relative ? current : root;
            return !query.execFrom(start, root).isEmpty();
        };
    }

    static ValueExpression queryValue(final Expression query, final boolean relative) {
        return (current, root) -> {
            JsonElement start = relative ? current : root;
            List<JsonElement> values = query.execFrom(start, root);
            return values.isEmpty() ? FilterValue.NOTHING : FilterValue.of(values.get(0));
        };
    }

    static ValueExpression literal(final JsonElement value) {
        return (current, root) -> FilterValue.of(value);
    }

    static FilterExpression comparison(final ValueExpression left, final String operator,
            final ValueExpression right) {
        return (current, root) -> {
            FilterValue leftValue = left.evaluate(current, root);
            FilterValue rightValue = right.evaluate(current, root);
            boolean equal = equals(leftValue, rightValue);
            if ("==".equals(operator)) {
                return equal;
            }
            if ("!=".equals(operator)) {
                return !equal;
            }

            Integer order = compare(leftValue, rightValue);
            if ("<".equals(operator)) {
                return order != null && order < 0;
            }
            if ("<=".equals(operator)) {
                return equal || order != null && order < 0;
            }
            if (">".equals(operator)) {
                return order != null && order > 0;
            }
            return equal || order != null && order > 0;
        };
    }

    private static boolean equals(FilterValue left, FilterValue right) {
        if (!left.present || !right.present) {
            return left.present == right.present;
        }
        return jsonEquals(left.value, right.value);
    }

    private static boolean jsonEquals(JsonElement left, JsonElement right) {
        if (left == null || right == null) {
            return left == right;
        }
        if (left.isJsonNull() || right.isJsonNull()) {
            return left.isJsonNull() && right.isJsonNull();
        }
        if (left.isJsonPrimitive() && right.isJsonPrimitive()) {
            JsonPrimitive leftPrimitive = left.getAsJsonPrimitive();
            JsonPrimitive rightPrimitive = right.getAsJsonPrimitive();
            if (leftPrimitive.isNumber() && rightPrimitive.isNumber()) {
                return numbersEqual(leftPrimitive, rightPrimitive);
            }
            if (leftPrimitive.isString() && rightPrimitive.isString()) {
                return leftPrimitive.getAsString().equals(rightPrimitive.getAsString());
            }
            if (leftPrimitive.isBoolean() && rightPrimitive.isBoolean()) {
                return leftPrimitive.getAsBoolean() == rightPrimitive.getAsBoolean();
            }
            return false;
        }
        if (left.isJsonArray() && right.isJsonArray()) {
            JsonArray leftArray = left.getAsJsonArray();
            JsonArray rightArray = right.getAsJsonArray();
            if (leftArray.size() != rightArray.size()) {
                return false;
            }
            for (int index = 0; index < leftArray.size(); index++) {
                if (!jsonEquals(leftArray.get(index), rightArray.get(index))) {
                    return false;
                }
            }
            return true;
        }
        if (left.isJsonObject() && right.isJsonObject()) {
            JsonObject leftObject = left.getAsJsonObject();
            JsonObject rightObject = right.getAsJsonObject();
            if (leftObject.size() != rightObject.size()) {
                return false;
            }
            for (Entry<String, JsonElement> entry : leftObject.entrySet()) {
                if (!rightObject.has(entry.getKey())
                        || !jsonEquals(entry.getValue(), rightObject.get(entry.getKey()))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static Integer compare(FilterValue left, FilterValue right) {
        if (!left.present || !right.present
                || left.value == JsonNull.INSTANCE || right.value == JsonNull.INSTANCE
                || !left.value.isJsonPrimitive() || !right.value.isJsonPrimitive()) {
            return null;
        }
        JsonPrimitive leftPrimitive = left.value.getAsJsonPrimitive();
        JsonPrimitive rightPrimitive = right.value.getAsJsonPrimitive();
        if (leftPrimitive.isNumber() && rightPrimitive.isNumber()) {
            try {
                return decimal(leftPrimitive).compareTo(decimal(rightPrimitive));
            } catch (NumberFormatException exception) {
                return null;
            }
        }
        if (leftPrimitive.isString() && rightPrimitive.isString()) {
            return compareUnicode(leftPrimitive.getAsString(), rightPrimitive.getAsString());
        }
        return null;
    }

    private static boolean numbersEqual(JsonPrimitive left, JsonPrimitive right) {
        try {
            return decimal(left).compareTo(decimal(right)) == 0;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static BigDecimal decimal(JsonPrimitive value) {
        return new BigDecimal(value.getAsString());
    }

    private static int compareUnicode(String left, String right) {
        int leftOffset = 0;
        int rightOffset = 0;
        while (leftOffset < left.length() && rightOffset < right.length()) {
            int leftCodePoint = left.codePointAt(leftOffset);
            int rightCodePoint = right.codePointAt(rightOffset);
            if (leftCodePoint != rightCodePoint) {
                return leftCodePoint < rightCodePoint ? -1 : 1;
            }
            leftOffset += Character.charCount(leftCodePoint);
            rightOffset += Character.charCount(rightCodePoint);
        }
        return Integer.compare(left.length() - leftOffset, right.length() - rightOffset);
    }
}
