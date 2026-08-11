lexer grammar JsonPathLexer;

ROOT: '$';
CURRENT: '@';
RECURSIVE_DESCENT: '..';
DOT: '.';
LEFT_BRACKET: '[';
RIGHT_BRACKET: ']';
LEFT_PARENTHESIS: '(';
RIGHT_PARENTHESIS: ')';
COLON: ':';
COMMA: ',';
WILDCARD: '*';
FILTER: '?';
NOT: '!';
OR: '||';
AND: '&&';
EQUAL: '==';
NOT_EQUAL: '!=';
LESS_OR_EQUAL: '<=';
GREATER_OR_EQUAL: '>=';
LESS: '<';
GREATER: '>';
LENGTH: 'length';
COUNT: 'count';
MATCH: 'match';
SEARCH: 'search';
VALUE: 'value';
TRUE: 'true';
FALSE: 'false';
NULL: 'null';
INTEGER: '0' | '-'? [1-9] [0-9]*;
NUMBER: ('-0' | INTEGER) ('.' [0-9]+)? ([eE] [+-]? [0-9]+)?;
SINGLE_QUOTED_STRING: '\'' SINGLE_QUOTED_CHARACTER* '\'';
DOUBLE_QUOTED_STRING: '"' DOUBLE_QUOTED_CHARACTER* '"';
IDENTIFIER: NAME_FIRST NAME_CHAR*;
WHITESPACE: [ \t\r\n]+ -> skip;

fragment SINGLE_QUOTED_CHARACTER
    : UNESCAPED_STRING_CHARACTER
    | '"'
    | '\\' '\''
    | ESCAPED_STRING_CHARACTER
    ;
fragment DOUBLE_QUOTED_CHARACTER
    : UNESCAPED_STRING_CHARACTER
    | '\''
    | '\\' '"'
    | ESCAPED_STRING_CHARACTER
    ;
fragment ESCAPED_STRING_CHARACTER
    : '\\' [bfnrt/\\]
    | '\\' 'u' UNICODE_ESCAPE_VALUE
    ;
fragment UNICODE_ESCAPE_VALUE
    : NON_SURROGATE_HEX_VALUE
    | HIGH_SURROGATE_HEX_VALUE '\\' 'u' LOW_SURROGATE_HEX_VALUE
    ;
fragment NON_SURROGATE_HEX_VALUE
    : [0-9a-cA-Ce-fE-F] HEX_DIGIT HEX_DIGIT HEX_DIGIT
    | [dD] [0-7] HEX_DIGIT HEX_DIGIT
    ;
fragment HIGH_SURROGATE_HEX_VALUE: [dD] [89aAbB] HEX_DIGIT HEX_DIGIT;
fragment LOW_SURROGATE_HEX_VALUE: [dD] [c-fC-F] HEX_DIGIT HEX_DIGIT;
fragment HEX_DIGIT: [0-9a-fA-F];
fragment UNESCAPED_STRING_CHARACTER
    : [\u0020-\u0021]
    | [\u0023-\u0026]
    | [\u0028-\u005B]
    | [\u005D-\uD7FF]
    | BMP_AFTER_SURROGATES
    | SUPPLEMENTARY_PLANE_SCALAR
    ;
fragment NAME_FIRST
    : ASCII_LETTER
    | '_'
    | NON_ASCII_BMP_BEFORE_SURROGATES
    | BMP_AFTER_SURROGATES
    | SUPPLEMENTARY_PLANE_SCALAR
    ;
fragment NAME_CHAR: NAME_FIRST | ASCII_DIGIT;

fragment ASCII_LETTER: [a-zA-Z];
fragment ASCII_DIGIT: [0-9];

// U+0080..U+D7FF: non-ASCII Basic Multilingual Plane before the surrogate block.
fragment NON_ASCII_BMP_BEFORE_SURROGATES: [\u0080-\uD7FF];

// U+E000..U+FFFF: Basic Multilingual Plane after the surrogate block.
fragment BMP_AFTER_SURROGATES: [\uE000-\uFFFF];

// U+10000..U+10FFFF: valid scalar values in the Unicode supplementary planes.
fragment SUPPLEMENTARY_PLANE_SCALAR: [\u{10000}-\u{10FFFF}];
