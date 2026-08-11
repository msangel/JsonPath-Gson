package net.sinistersky.j2ee.support;

import java.util.ArrayList;
import java.util.List;

import net.sinistersky.j2ee.support.antlr.JsonPathLexer;
import net.sinistersky.j2ee.support.antlr.JsonPathParser.BracketSelectorContext;
import net.sinistersky.j2ee.support.antlr.JsonPathParser.IndexSelectorContext;
import net.sinistersky.j2ee.support.antlr.JsonPathParser.PropertySelectorContext;
import net.sinistersky.j2ee.support.antlr.JsonPathParser.SelectorContext;
import net.sinistersky.j2ee.support.antlr.JsonPathParser.SliceSelectorContext;
import net.sinistersky.j2ee.support.antlr.JsonPathParser.WildcardSelectorContext;
import net.sinistersky.j2ee.support.antlr.JsonPathParserBaseVisitor;
import net.sinistersky.j2ee.support.nodetypes.ArrayIndexPathNode;
import net.sinistersky.j2ee.support.nodetypes.CSVIndexPathNode;
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
        public Void visitPathSegment(
                net.sinistersky.j2ee.support.antlr.JsonPathParser.PathSegmentContext context) {
            if (context.RECURSIVE_DESCENT() != null) {
                nodes.add(new RecursiveDescentPathNode());
            }
            if (context.memberName() != null) {
                if (context.memberName().WILDCARD() != null) {
                    nodes.add(new WildcardPathNode());
                } else {
                    nodes.add(new NamedPropertyPathNode(context.memberName().IDENTIFIER().getText()));
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
                return new NamedPropertyPathNode(unquote(property.quotedName().getText()));
            }
            if (context instanceof IndexSelectorContext) {
                IndexSelectorContext index = (IndexSelectorContext) context;
                return new ArrayIndexPathNode(parseInteger(index.INTEGER().getText()));
            }
            SliceSelectorContext slice = (SliceSelectorContext) context;
            return createSlice(slice);
        }

        private static SlicePathNode createSlice(SliceSelectorContext context) {
            String[] parts = context.getText().split(":", -1);
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

        private static String unquote(String value) {
            StringBuilder result = new StringBuilder();
            for (int index = 1; index < value.length() - 1; index++) {
                char current = value.charAt(index);
                if (current != '\\') {
                    result.append(current);
                    continue;
                }
                char escaped = value.charAt(++index);
                switch (escaped) {
                    case 'b':
                        result.append('\b');
                        break;
                    case 'f':
                        result.append('\f');
                        break;
                    case 'n':
                        result.append('\n');
                        break;
                    case 'r':
                        result.append('\r');
                        break;
                    case 't':
                        result.append('\t');
                        break;
                    case 'u':
                        index = appendUnicodeEscape(value, index + 1, result);
                        break;
                    default:
                        result.append(escaped);
                        break;
                }
            }
            return result.toString();
        }

        private static int appendUnicodeEscape(String value, int hexStart, StringBuilder result) {
            char first = (char) Integer.parseInt(value.substring(hexStart, hexStart + 4), 16);
            int lastConsumed = hexStart + 3;
            if (Character.isHighSurrogate(first)) {
                int lowStart = hexStart + 6;
                char second = (char) Integer.parseInt(value.substring(lowStart, lowStart + 4), 16);
                result.appendCodePoint(Character.toCodePoint(first, second));
                lastConsumed = lowStart + 3;
            } else {
                result.append(first);
            }
            return lastConsumed;
        }
    }
}
