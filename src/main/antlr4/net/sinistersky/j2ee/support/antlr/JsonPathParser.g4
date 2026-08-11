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
    : IDENTIFIER
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
    ;

quotedName
    : SINGLE_QUOTED_STRING
    | DOUBLE_QUOTED_STRING
    ;
