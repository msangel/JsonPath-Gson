parser grammar JsonPathParser;

options {
    tokenVocab = JsonPathLexer;
}

jsonPath
    : ROOT pathSegment* EOF
    ;

pathSegment
    : DOT memberName
    | RECURSIVE_DESCENT (memberName | bracketSelector)
    | DOT? bracketSelector
    ;

memberName
    : identifier
    | WILDCARD
    ;

bracketSelector
    : LEFT_BRACKET selector (COMMA selector)* RIGHT_BRACKET
    ;

selector
    : WILDCARD                                      # wildcardSelector
    | quotedName                                    # propertySelector
    | INTEGER                                       # indexSelector
    | INTEGER? COLON INTEGER? (COLON INTEGER?)?     # sliceSelector
    | FILTER logicalExpression                      # filterSelector
    ;

quotedName
    : SINGLE_QUOTED_STRING
    | DOUBLE_QUOTED_STRING
    ;

logicalExpression
    : logicalAndExpression (OR logicalAndExpression)*
    ;

logicalAndExpression
    : basicExpression (AND basicExpression)*
    ;

basicExpression
    : NOT? LEFT_PARENTHESIS logicalExpression RIGHT_PARENTHESIS # parenthesizedExpression
    | comparisonExpression                                      # comparisonBasicExpression
    | NOT? logicalFunctionExpression                            # functionTestExpression
    | NOT? query                                                # testExpression
    ;

comparisonExpression
    : comparable comparisonOperator comparable
    ;

comparable
    : literal
    | singularQuery
    | valueFunctionExpression
    ;

literal
    : SINGLE_QUOTED_STRING
    | DOUBLE_QUOTED_STRING
    | INTEGER
    | NUMBER
    | TRUE
    | FALSE
    | NULL
    ;

comparisonOperator
    : EQUAL
    | NOT_EQUAL
    | LESS
    | LESS_OR_EQUAL
    | GREATER
    | GREATER_OR_EQUAL
    ;

query
    : (ROOT | CURRENT) pathSegment*
    ;

singularQuery
    : (ROOT | CURRENT) singularPathSegment*
    ;

singularPathSegment
    : DOT identifier
    | LEFT_BRACKET (quotedName | INTEGER) RIGHT_BRACKET
    ;

valueExpression
    : literal
    | singularQuery
    | valueFunctionExpression
    ;

nodesExpression
    : query
    ;

valueFunctionExpression
    : LENGTH LEFT_PARENTHESIS valueExpression RIGHT_PARENTHESIS # lengthFunctionExpression
    | COUNT LEFT_PARENTHESIS nodesExpression RIGHT_PARENTHESIS  # countFunctionExpression
    | VALUE LEFT_PARENTHESIS nodesExpression RIGHT_PARENTHESIS  # valueFunctionExpressionCall
    ;

logicalFunctionExpression
    : MATCH LEFT_PARENTHESIS valueExpression COMMA valueExpression RIGHT_PARENTHESIS
        # matchFunctionExpression
    | SEARCH LEFT_PARENTHESIS valueExpression COMMA valueExpression RIGHT_PARENTHESIS
        # searchFunctionExpression
    ;

identifier
    : IDENTIFIER
    | LENGTH
    | COUNT
    | MATCH
    | SEARCH
    | VALUE
    | TRUE
    | FALSE
    | NULL
    ;
