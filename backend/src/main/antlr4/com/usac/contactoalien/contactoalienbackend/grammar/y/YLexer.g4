
lexer grammar YLexer;

options {
    superClass = YIndentLexerBase;
}

tokens { INDENT, DEDENT }


ESTRUCTURA : 'estructura';
DEFINIR    : 'definir';
RETORNAR   : 'retornar';
SI         : 'si';
ENTONCES   : 'entonces';
SINO       : 'sino';
CONTRARIO  : 'contrario';
ELEGIR     : 'elegir';
CASO       : 'caso';
SIEMPRE    : 'siempre';
ROMPER     : 'romper';
PARA       : 'para';
MIENTRAS   : 'mientras';
HACER      : 'hacer';
CONTINUAR  : 'continuar';
IMPRIMIR   : 'imprimir';
LEER       : 'leer';
ENTERO     : 'entero';
CADENA     : 'cadena';
FLOTANTE   : 'flotante';
CARACTER   : 'caracter';
BOOL       : 'bool';
VERDADERO  : 'verdadero';
FALSO      : 'falso';

SECCION_ESTRUCTURAS : '%estructuras';
SECCION_FUNCIONES   : '%funciones';

FLOAT_LITERAL
    : DIGIT+ '.' DIGIT+
    ;

INT_LITERAL
    : DIGIT+
    ;

CHAR_LITERAL
    : '\'' (ESCAPE_SEQ | ~['\\]) '\''
    ;

STRING_LITERAL
    : '"' (ESCAPE_SEQ | ~["\\])* '"'
    ;

IDENTIFIER
    : [a-zA-Z_] [a-zA-Z0-9_]*
    ;

PLUS   : '+';
MINUS  : '-';
STAR   : '*';
DIV    : '/';
EQ     : '==';
NEQ    : '!=';
LT     : '<';
GT     : '>';
LE     : '<=';
GE     : '>=';
AND    : '&&';
OR     : '||';
NOT    : '!';
INC    : '++';
DEC    : '--';
ASSIGN : '=';
ARROW  : '->';
COLON  : ':';
COMMA  : ',';
DOT    : '.';
LBRACK : '[';
RBRACK : ']';
LBRACE : '{';
RBRACE : '}';
LPAREN : '(';
RPAREN : ')';
SEMI   : ';';

LINE_COMMENT  : '//' ~[\r\n]*  -> channel(HIDDEN);
BLOCK_COMMENT : '/*' .*? '*/'  -> channel(HIDDEN);

NEWLINE : ('\r'? '\n' | '\r') ' '*;

WS : [ \t]+ -> skip;

fragment DIGIT      : [0-9];
fragment ESCAPE_SEQ : '\\' ('"' | 'n' | '\\');