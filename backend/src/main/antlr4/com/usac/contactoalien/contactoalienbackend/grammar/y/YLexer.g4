
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
    : '"' (ESCAPE_SEQ | ~["\\\r\n])* '"'
    ;

// An unterminated string is reported once and kept as a string token, so the parser does not cascade.
UNCLOSED_STRING
    : '"' (ESCAPE_SEQ | ~["\\\r\n])*
      { notifyListeners(new org.antlr.v4.runtime.LexerNoViableAltException(this, _input, _tokenStartCharIndex, null)); }
      -> type(STRING_LITERAL)
    ;

IDENTIFIER
    : [a-zA-Z_] [a-zA-Z0-9_]*
    ;

PLUS   : '+';
MINUS  : '-';
STAR   : '*';
DIV    : '/';
MOD    : '%';
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

// A block comment that never closes is reported as one lexical error and skipped, instead of
// leaving a stray '/' for the parser to complain about.
UNCLOSED_COMMENT
    : '/*' ( ~'*' | '*'+ ~[*/] )* '*'*
      { notifyListeners(new org.antlr.v4.runtime.LexerNoViableAltException(this, _input, _tokenStartCharIndex, null)); }
      -> skip
    ;

NEWLINE : ('\r'? '\n' | '\r') [ \t]*;

// \uFEFF is the byte order mark some editors put at the start of a UTF-8 file.
WS : [ \t\uFEFF]+ -> skip;

fragment DIGIT      : [0-9];
fragment ESCAPE_SEQ : '\\' ('"' | '\'' | 'n' | 't' | 'r' | '\\');