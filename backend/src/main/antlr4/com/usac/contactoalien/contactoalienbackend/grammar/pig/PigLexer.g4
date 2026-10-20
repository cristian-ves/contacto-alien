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

// Section markers, each a single token including '>'
VARIABILES_SECTION : 'VARIABILES>';
MAIOR_SECTION       : 'MAIOR>';

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

fragment EscapeSeq : '\\' ["'\\n];

IDENTIFIER : [a-zA-Z_][a-zA-Z0-9_]*;

// Block comment: opening '##' line ... closing '##' line, sent to HIDDEN
LINE_COMMENT  : '//' ~[\r\n]* -> channel(HIDDEN);
BLOCK_COMMENT : '##' .*? '##' -> channel(HIDDEN);

WS : [ \t\r\n]+ -> skip;