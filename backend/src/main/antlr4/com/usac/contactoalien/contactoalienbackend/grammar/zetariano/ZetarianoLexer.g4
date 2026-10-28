lexer grammar ZetarianoLexer;

PUBLIC    : 'public';
PRIVATE   : 'private';
PROTECTED : 'protected';
EXTENDS   : 'extends';
SUPER     : 'super';
OVERRIDE  : '@Override';
CLASS     : 'class';
INT       : 'int';
DOUBLE    : 'double';
CHAR      : 'char';
BOOLEAN   : 'boolean';
STRING    : 'String';
VOID      : 'void';
NEW       : 'new';
RETURN    : 'return';
IF        : 'if';
ELSE      : 'else';
WHILE     : 'while';
DO        : 'do';
FOR       : 'for';
SWITCH    : 'switch';
CASE      : 'case';
DEFAULT   : 'default';
BREAK     : 'break';
CONTINUE  : 'continue';
TRUE      : 'true';
FALSE     : 'false';
NULL      : 'null';
THIS      : 'this';
PRINTLN   : 'println';
PRINT     : 'print';
READLN    : 'readln';

PLUS_PLUS    : '++';
MINUS_MINUS  : '--';
PLUS_ASSIGN  : '+=';
MINUS_ASSIGN : '-=';
MUL_ASSIGN   : '*=';
EQ           : '==';
NEQ          : '!=';
LE           : '<=';
GE           : '>=';
AND          : '&&';
OR           : '||';

PLUS      : '+';
MINUS     : '-';
STAR      : '*';
DIV       : '/';
MOD       : '%';
LT        : '<';
GT        : '>';
NOT       : '!';
ASSIGN    : '=';
QUESTION  : '?';
COLON     : ':';

LBRACE    : '{';
RBRACE    : '}';
LPAREN    : '(';
RPAREN    : ')';
LBRACKET  : '[';
RBRACKET  : ']';
SEMI      : ';';
COMMA     : ',';
DOT       : '.';

DOUBLE_LITERAL : [0-9]+ '.' [0-9]+;
INT_LITERAL    : [0-9]+;

CHAR_LITERAL
    : '\'' (ESCAPE_SEQUENCE | ~['\\]) '\''
    ;

STRING_LITERAL
    : '"' (ESCAPE_SEQUENCE | ~["\\\r\n])* '"'
    ;

// An unterminated string is reported once and kept as a string token, so the parser does not cascade.
UNCLOSED_STRING
    : '"' (ESCAPE_SEQUENCE | ~["\\\r\n])*
      { notifyListeners(new org.antlr.v4.runtime.LexerNoViableAltException(this, _input, _tokenStartCharIndex, null)); }
      -> type(STRING_LITERAL)
    ;

fragment ESCAPE_SEQUENCE
    : '\\' ('"' | '\'' | 'n' | 't' | 'r' | '\\')
    ;

IDENTIFIER : [a-zA-Z_][a-zA-Z0-9_]*;

LINE_COMMENT  : '//' ~[\r\n]* -> channel(HIDDEN);
BLOCK_COMMENT : '/*' .*? '*/' -> channel(HIDDEN);

// A block comment that never closes is reported as one lexical error and skipped, instead of
// leaving a stray '/' for the parser to complain about.
UNCLOSED_COMMENT
    : '/*' ( ~'*' | '*'+ ~[*/] )* '*'*
      { notifyListeners(new org.antlr.v4.runtime.LexerNoViableAltException(this, _input, _tokenStartCharIndex, null)); }
      -> skip
    ;

// \uFEFF is the byte order mark some editors put at the start of a UTF-8 file.
WS : [ \t\r\n\uFEFF]+ -> skip;