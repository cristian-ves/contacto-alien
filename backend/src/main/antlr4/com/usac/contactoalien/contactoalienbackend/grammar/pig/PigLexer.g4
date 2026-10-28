lexer grammar PigLexer;

// Reserved words (case sensitive)
IMPORT      : 'import';
ESTO        : 'esto';
NOVUS       : 'novus';
SI          : 'si';
ALITER      : 'aliter';
FINIS       : 'finis';
NUMERUS     : 'numerus';
TEXTUM      : 'textum';
DECIMALIS   : 'decimalis';
LITTERA     : 'littera';
BOOL        : 'bool';
SERIES      : 'series';
DUM         : 'dum';
FACERE      : 'facere';
PER         : 'per';
PERGE       : 'perge';
INTERRUMPE  : 'interrumpe';
VERUM       : 'verum';
FALSUS      : 'falsus';
NON         : 'non';
NULL        : 'null';

// Section markers, each a single token including '>'
VARIABILES_SECTION : 'VARIABILES>';
MAIOR_SECTION       : 'MAIOR>';
// Functions are declared in .y files; the body of a MUNERA> section is skipped so only one clear error is reported.
FUNCTIONS_SECTION   : 'MUNERA>' -> pushMode(FUNCTIONS_BODY);

// Uppercase end-of-program marker, distinct token from lowercase FINIS
PROGRAM_END : 'FINIS';

// Two-character print/read operators
PRINT_OP : '>>';
READ_OP  : '<<';

// Relational and logical operators
EQ  : '==';
NEQ : '!=';
LE  : '<=';
GE  : '>=';
AND : '&&';
OR  : '||';
LT  : '<';
GT  : '>';
NOT : '!';

// Arithmetic operators
INC   : '++';
DEC   : '--';
PLUS  : '+';
MINUS : '-';
STAR  : '*';
DIV   : '/';
MOD   : '%';

// Delimiters
COLON  : ':';
SEMI   : ';';
COMMA  : ',';
DOT    : '.';
LBRACK : '[';
RBRACK : ']';
LBRACE : '{';
RBRACE : '}';
LPAREN : '(';
RPAREN : ')';
ASSIGN : '=';

// Literals
FLOAT_LITERAL  : [0-9]+ '.' [0-9]+;
INT_LITERAL    : [0-9]+;
CHAR_LITERAL   : '\'' (EscapeSeq | ~['\\\r\n]) '\'';
STRING_LITERAL : '"' (EscapeSeq | ~["\\\r\n])* '"';
// An unterminated string is reported once and kept as a string token, so the parser does not cascade.
UNCLOSED_STRING : '"' (EscapeSeq | ~["\\\r\n])*
    { notifyListeners(new org.antlr.v4.runtime.LexerNoViableAltException(this, _input, _tokenStartCharIndex, null)); }
    -> type(STRING_LITERAL);

fragment EscapeSeq : '\\' ["'\\ntr];

IDENTIFIER : [a-zA-Z_][a-zA-Z0-9_]*;

// Line comment: '//' up to the end of the line, sent to HIDDEN
LINE_COMMENT  : '//' ~[\r\n]* -> channel(HIDDEN);
// Block comment: opening '##' line ... closing '##' line, sent to HIDDEN
BLOCK_COMMENT : '##' .*? '##' -> channel(HIDDEN);

// \uFEFF is the byte order mark some editors put at the start of a UTF-8 file.
WS : [ \t\r\n\uFEFF]+ -> skip;

mode FUNCTIONS_BODY;

FUNCTIONS_END  : 'MAIOR>' -> type(MAIOR_SECTION), popMode;
FUNCTIONS_SKIP : . -> skip;
