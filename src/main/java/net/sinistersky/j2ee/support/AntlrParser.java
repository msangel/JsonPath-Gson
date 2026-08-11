package net.sinistersky.j2ee.support;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import net.sinistersky.j2ee.support.antlr.JsonPathLexer;
import net.sinistersky.j2ee.support.antlr.JsonPathParser.BasicExpressionContext;
import net.sinistersky.j2ee.support.antlr.JsonPathParser.BracketSelectorContext;
import net.sinistersky.j2ee.support.antlr.JsonPathParser.ComparableContext;
import net.sinistersky.j2ee.support.antlr.JsonPathParser.ComparisonBasicExpressionContext;
import net.sinistersky.j2ee.support.antlr.JsonPathParser.ComparisonExpressionContext;
import net.sinistersky.j2ee.support.antlr.JsonPathParser.CountFunctionExpressionContext;
import net.sinistersky.j2ee.support.antlr.JsonPathParser.FilterSelectorContext;
import net.sinistersky.j2ee.support.antlr.JsonPathParser.FunctionTestExpressionContext;
import net.sinistersky.j2ee.support.antlr.JsonPathParser.IndexSelectorContext;
import net.sinistersky.j2ee.support.antlr.JsonPathParser.LengthFunctionExpressionContext;
import net.sinistersky.j2ee.support.antlr.JsonPathParser.LiteralContext;
import net.sinistersky.j2ee.support.antlr.JsonPathParser.LogicalAndExpressionContext;
import net.sinistersky.j2ee.support.antlr.JsonPathParser.LogicalExpressionContext;
import net.sinistersky.j2ee.support.antlr.JsonPathParser.MatchFunctionExpressionContext;
import net.sinistersky.j2ee.support.antlr.JsonPathParser.NodesExpressionContext;
import net.sinistersky.j2ee.support.antlr.JsonPathParser.ParenthesizedExpressionContext;
import net.sinistersky.j2ee.support.antlr.JsonPathParser.PathSegmentContext;
import net.sinistersky.j2ee.support.antlr.JsonPathParser.PropertySelectorContext;
import net.sinistersky.j2ee.support.antlr.JsonPathParser.QueryContext;
import net.sinistersky.j2ee.support.antlr.JsonPathParser.SearchFunctionExpressionContext;
import net.sinistersky.j2ee.support.antlr.JsonPathParser.SelectorContext;
import net.sinistersky.j2ee.support.antlr.JsonPathParser.SingularPathSegmentContext;
import net.sinistersky.j2ee.support.antlr.JsonPathParser.SingularQueryContext;
import net.sinistersky.j2ee.support.antlr.JsonPathParser.SliceSelectorContext;
import net.sinistersky.j2ee.support.antlr.JsonPathParser.TestExpressionContext;
import net.sinistersky.j2ee.support.antlr.JsonPathParser.ValueExpressionContext;
import net.sinistersky.j2ee.support.antlr.JsonPathParser.ValueFunctionExpressionCallContext;
import net.sinistersky.j2ee.support.antlr.JsonPathParser.WildcardSelectorContext;
import net.sinistersky.j2ee.support.antlr.JsonPathParserBaseVisitor;
import net.sinistersky.j2ee.support.nodetypes.ArrayIndexPathNode;
import net.sinistersky.j2ee.support.nodetypes.CSVIndexPathNode;
import net.sinistersky.j2ee.support.nodetypes.FilterPathNode;
import net.sinistersky.j2ee.support.nodetypes.NamedPropertyPathNode;
import net.sinistersky.j2ee.support.nodetypes.PathNode;
import net.sinistersky.j2ee.support.nodetypes.RecursiveDescentPathNode;
import net.sinistersky.j2ee.support.nodetypes.SelectorListPathNode;
import net.sinistersky.j2ee.support.nodetypes.SlicePathNode;
import net.sinistersky.j2ee.support.nodetypes.WildcardPathNode;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

class AntlrParser {

    private static final long MAX_I_JSON_INTEGER = 9007199254740991L;

    private static final BaseErrorListener ERROR_LISTENER = new BaseErrorListener() {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                int line, int charPositionInLine, String message, RecognitionException exception) {
            throw new JsonPathException("invalid expression syntax at " + line + ":"
                    + charPositionInLine + ": " + message);
        }
    };

    Expression parseExpression(String str) throws JsonPathException {
        validateInput(str);

        JsonPathLexer lexer = new JsonPathLexer(CharStreams.fromString(str));
        lexer.removeErrorListeners();
        lexer.addErrorListener(ERROR_LISTENER);

        net.sinistersky.j2ee.support.antlr.JsonPathParser parser =
                new net.sinistersky.j2ee.support.antlr.JsonPathParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(ERROR_LISTENER);

        PathNodeVisitor visitor = new PathNodeVisitor();
        visitor.visit(parser.jsonPath());
        return new Expression(visitor.getNodes());
    }

    private void validateInput(String str) {
        if (str == null) {
            throw new JsonPathException("null input");
        }
        if (str.isEmpty()) {
            throw new JsonPathException("empty string");
        }
        if (Character.isWhitespace(str.charAt(0))
                || Character.isWhitespace(str.charAt(str.length() - 1))) {
            throw new JsonPathException("input string should be trimmed");
        }
    }

    private static final class PathNodeVisitor extends JsonPathParserBaseVisitor<Void> {
        private final List<PathNode> nodes = new ArrayList<>();

        List<PathNode> getNodes() {
            return nodes;
        }

        @Override
        public Void visitPathSegment(PathSegmentContext context) {
            if (context.RECURSIVE_DESCENT() != null) {
                nodes.add(new RecursiveDescentPathNode());
            }
            if (context.memberName() != null) {
                if (context.memberName().WILDCARD() != null) {
                    nodes.add(new WildcardPathNode());
                } else {
                    nodes.add(new NamedPropertyPathNode(context.memberName().getText()));
                }
            } else {
                nodes.add(createBracketSelector(context.bracketSelector()));
            }
            return null;
        }

        private static PathNode createBracketSelector(BracketSelectorContext context) {
            List<PathNode> selectors = new ArrayList<>();
            boolean onlyIndexes = true;
            for (SelectorContext selector : context.selector()) {
                PathNode node = createSelector(selector);
                selectors.add(node);
                if (!(node instanceof ArrayIndexPathNode)) {
                    onlyIndexes = false;
                }
            }
            if (selectors.size() == 1) {
                return selectors.get(0);
            }
            if (onlyIndexes) {
                List<Long> indexes = new ArrayList<>();
                for (SelectorContext selector : context.selector()) {
                    indexes.add(parseInteger(selector.getText()));
                }
                return new CSVIndexPathNode(indexes);
            }
            return new SelectorListPathNode(selectors);
        }

        private static PathNode createSelector(SelectorContext context) {
            if (context instanceof WildcardSelectorContext) {
                return new WildcardPathNode();
            }
            if (context instanceof PropertySelectorContext) {
                PropertySelectorContext property = (PropertySelectorContext) context;
                return new NamedPropertyPathNode(
                        JsonPathStringDecoder.unquote(property.quotedName().getText()));
            }
            if (context instanceof IndexSelectorContext) {
                IndexSelectorContext index = (IndexSelectorContext) context;
                return new ArrayIndexPathNode(parseInteger(index.INTEGER().getText()));
            }
            if (context instanceof FilterSelectorContext) {
                return new FilterPathNode(
                        (FilterExpression) new FilterExpressionVisitor().visit(
                                ((FilterSelectorContext) context).logicalExpression()));
            }
            SliceSelectorContext slice = (SliceSelectorContext) context;
            return createSlice(slice);
        }

        private static SlicePathNode createSlice(SliceSelectorContext context) {
            String[] parts = context.getText().replaceAll("[ \\t\\r\\n]", "").split(":", -1);
            Long from = parseOptionalInteger(parts[0]);
            Long to = parseOptionalInteger(parts[1]);
            Long step = parts.length == 3 ? parseOptionalInteger(parts[2]) : null;
            return new SlicePathNode(from, to, step);
        }

        private static Long parseOptionalInteger(String value) {
            return value.isEmpty() ? null : parseInteger(value);
        }

        private static long parseInteger(String value) {
            try {
                long parsed = Long.parseLong(value);
                if (parsed < -MAX_I_JSON_INTEGER || parsed > MAX_I_JSON_INTEGER) {
                    throw new JsonPathException("integer value is outside the I-JSON range: " + value);
                }
                return parsed;
            } catch (NumberFormatException exception) {
                throw new JsonPathException("integer value is outside the I-JSON range: " + value);
            }
        }

    }

    private static final class FilterExpressionVisitor extends JsonPathParserBaseVisitor<Object> {

        @Override
        public Object visitLogicalExpression(LogicalExpressionContext context) {
            List<FilterExpression> expressions = new ArrayList<>();
            for (LogicalAndExpressionContext child : context.logicalAndExpression()) {
                expressions.add((FilterExpression) visit(child));
            }
            return FilterExpressions.or(expressions);
        }

        @Override
        public Object visitLogicalAndExpression(LogicalAndExpressionContext context) {
            List<FilterExpression> expressions = new ArrayList<>();
            for (BasicExpressionContext child : context.basicExpression()) {
                expressions.add((FilterExpression) visit(child));
            }
            return FilterExpressions.and(expressions);
        }

        @Override
        public Object visitParenthesizedExpression(ParenthesizedExpressionContext context) {
            FilterExpression expression = (FilterExpression) visit(context.logicalExpression());
            return context.NOT() == null ? expression : FilterExpressions.not(expression);
        }

        @Override
        public Object visitComparisonBasicExpression(ComparisonBasicExpressionContext context) {
            return visit(context.comparisonExpression());
        }

        @Override
        public Object visitTestExpression(TestExpressionContext context) {
            ParsedQuery query = parseQuery(context.query());
            FilterExpression expression =
                    FilterExpressions.queryTest(query.expression, query.relative);
            return context.NOT() == null ? expression : FilterExpressions.not(expression);
        }

        @Override
        public Object visitFunctionTestExpression(FunctionTestExpressionContext context) {
            FilterExpression expression =
                    (FilterExpression) visit(context.logicalFunctionExpression());
            return context.NOT() == null ? expression : FilterExpressions.not(expression);
        }

        @Override
        public Object visitComparisonExpression(ComparisonExpressionContext context) {
            FilterExpressions.ValueExpression left =
                    (FilterExpressions.ValueExpression) visit(context.comparable(0));
            FilterExpressions.ValueExpression right =
                    (FilterExpressions.ValueExpression) visit(context.comparable(1));
            return FilterExpressions.comparison(
                    left, context.comparisonOperator().getText(), right);
        }

        @Override
        public Object visitComparable(ComparableContext context) {
            if (context.literal() != null) {
                return visit(context.literal());
            }
            if (context.singularQuery() != null) {
                return queryValue(context.singularQuery());
            }
            return visit(context.valueFunctionExpression());
        }

        @Override
        public Object visitLiteral(LiteralContext context) {
            String text = context.getText();
            if (context.SINGLE_QUOTED_STRING() != null
                    || context.DOUBLE_QUOTED_STRING() != null) {
                return FilterExpressions.literal(
                        new JsonPrimitive(JsonPathStringDecoder.unquote(text)));
            }
            if (context.TRUE() != null || context.FALSE() != null) {
                return FilterExpressions.literal(new JsonPrimitive(Boolean.parseBoolean(text)));
            }
            if (context.NULL() != null) {
                return FilterExpressions.literal(JsonNull.INSTANCE);
            }
            return FilterExpressions.literal(new JsonPrimitive(new BigDecimal(text)));
        }

        @Override
        public Object visitValueExpression(ValueExpressionContext context) {
            if (context.literal() != null) {
                return visit(context.literal());
            }
            if (context.singularQuery() != null) {
                return queryValue(context.singularQuery());
            }
            return visit(context.valueFunctionExpression());
        }

        @Override
        public Object visitNodesExpression(NodesExpressionContext context) {
            ParsedQuery query = parseQuery(context.query());
            return FilterExpressions.queryNodes(query.expression, query.relative);
        }

        @Override
        public Object visitLengthFunctionExpression(LengthFunctionExpressionContext context) {
            return FilterExpressions.length(
                    (FilterExpressions.ValueExpression) visit(context.valueExpression()));
        }

        @Override
        public Object visitCountFunctionExpression(CountFunctionExpressionContext context) {
            return FilterExpressions.count(
                    (FilterExpressions.NodesExpression) visit(context.nodesExpression()));
        }

        @Override
        public Object visitValueFunctionExpressionCall(
                ValueFunctionExpressionCallContext context) {
            return FilterExpressions.value(
                    (FilterExpressions.NodesExpression) visit(context.nodesExpression()));
        }

        @Override
        public Object visitMatchFunctionExpression(MatchFunctionExpressionContext context) {
            return regularExpression(context.valueExpression(0), context.valueExpression(1), false);
        }

        @Override
        public Object visitSearchFunctionExpression(SearchFunctionExpressionContext context) {
            return regularExpression(context.valueExpression(0), context.valueExpression(1), true);
        }

        private Object regularExpression(ValueExpressionContext input,
                ValueExpressionContext regularExpression, boolean search) {
            return FilterExpressions.regularExpression(
                    (FilterExpressions.ValueExpression) visit(input),
                    (FilterExpressions.ValueExpression) visit(regularExpression), search);
        }

        private static FilterExpressions.ValueExpression queryValue(
                SingularQueryContext context) {
            ParsedQuery query = parseQuery(context);
            return FilterExpressions.queryValue(query.expression, query.relative);
        }

        private static ParsedQuery parseQuery(QueryContext context) {
            PathNodeVisitor visitor = new PathNodeVisitor();
            for (PathSegmentContext segment : context.pathSegment()) {
                visitor.visitPathSegment(segment);
            }
            return new ParsedQuery(
                    new Expression(visitor.getNodes()), context.CURRENT() != null);
        }

        private static ParsedQuery parseQuery(SingularQueryContext context) {
            List<PathNode> nodes = new ArrayList<>();
            for (SingularPathSegmentContext segment : context.singularPathSegment()) {
                if (segment.identifier() != null) {
                    nodes.add(new NamedPropertyPathNode(segment.identifier().getText()));
                } else if (segment.quotedName() != null) {
                    nodes.add(new NamedPropertyPathNode(
                            JsonPathStringDecoder.unquote(segment.quotedName().getText())));
                } else {
                    nodes.add(new ArrayIndexPathNode(
                            PathNodeVisitor.parseInteger(segment.INTEGER().getText())));
                }
            }
            return new ParsedQuery(new Expression(nodes), context.CURRENT() != null);
        }
    }

    private static final class ParsedQuery {
        private final Expression expression;
        private final boolean relative;

        private ParsedQuery(Expression expression, boolean relative) {
            this.expression = expression;
            this.relative = relative;
        }
    }
}
