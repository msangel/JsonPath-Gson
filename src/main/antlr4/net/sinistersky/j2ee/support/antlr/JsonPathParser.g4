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
    | NOT? query                                                # testExpression
    ;

comparisonExpression
    : comparable comparisonOperator comparable
    ;

comparable
    : literal
    | singularQuery
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

identifier
    : IDENTIFIER
    | TRUE
    | FALSE
    | NULL
    ;
