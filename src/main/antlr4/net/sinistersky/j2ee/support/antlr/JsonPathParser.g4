parser grammar JsonPathParser;

options {
    tokenVocab = JsonPathLexer;
}

jsonPath
    : ROOT (WS? pathSegment)* EOF
    ;

pathSegment
    : DOT memberName
    | RECURSIVE_DESCENT (memberName | bracketSelector)
    | bracketSelector
    ;

memberName
    : identifier
    | WILDCARD
    ;

bracketSelector
    : LEFT_BRACKET WS? selector
        (WS? COMMA WS? selector)* WS? RIGHT_BRACKET
    ;

selector
    : WILDCARD                                      # wildcardSelector
    | quotedName                                    # propertySelector
    | INTEGER                                       # indexSelector
    | (INTEGER WS?)? COLON WS?
        (INTEGER WS?)? (COLON (WS? INTEGER)?)? # sliceSelector
    | FILTER WS? logicalExpression              # filterSelector
    ;

quotedName
    : SINGLE_QUOTED_STRING
    | DOUBLE_QUOTED_STRING
    ;

logicalExpression
    : logicalAndExpression (WS? OR WS? logicalAndExpression)*
    ;

logicalAndExpression
    : basicExpression (WS? AND WS? basicExpression)*
    ;

basicExpression
    : (NOT WS?)? LEFT_PARENTHESIS WS?
        logicalExpression WS? RIGHT_PARENTHESIS # parenthesizedExpression
    | comparisonExpression                      # comparisonBasicExpression
    | (NOT WS?)? logicalFunctionExpression      # functionTestExpression
    | (NOT WS?)? query                          # testExpression
    ;

comparisonExpression
    : comparable WS? comparisonOperator WS? comparable
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
    : (ROOT | CURRENT) (WS? pathSegment)*
    ;

singularQuery
    : (ROOT | CURRENT) (WS? singularPathSegment)*
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
    : LENGTH LEFT_PARENTHESIS WS? valueExpression WS? RIGHT_PARENTHESIS
        # lengthFunctionExpression
    | COUNT LEFT_PARENTHESIS WS? nodesExpression WS? RIGHT_PARENTHESIS
        # countFunctionExpression
    | VALUE LEFT_PARENTHESIS WS? nodesExpression WS? RIGHT_PARENTHESIS
        # valueFunctionExpressionCall
    ;

logicalFunctionExpression
    : MATCH LEFT_PARENTHESIS WS? valueExpression WS?
        COMMA WS? valueExpression WS? RIGHT_PARENTHESIS
        # matchFunctionExpression
    | SEARCH LEFT_PARENTHESIS WS? valueExpression WS?
        COMMA WS? valueExpression WS? RIGHT_PARENTHESIS
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
