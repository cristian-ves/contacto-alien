lexer grammar ZetarianoLexer;

PUBLIC    : 'public';
PRIVATE   : 'private';
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
    : '"' (ESCAPE_SEQUENCE | ~["\\])* '"'
    ;

fragment ESCAPE_SEQUENCE
    : '\\' ('"' | 'n' | '\\')
    ;

IDENTIFIER : [a-zA-Z_][a-zA-Z0-9_]*;

LINE_COMMENT  : '//' ~[\r\n]* -> channel(HIDDEN);
BLOCK_COMMENT : '/*' .*? '*/' -> channel(HIDDEN);

WS : [ \t\r\n]+ -> skip;