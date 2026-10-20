
parser grammar YParser;

options {
    tokenVocab = YLexer;
}

program
    : NEWLINE* structuresSection? functionsSection EOF
    ;

structuresSection
    : SECCION_ESTRUCTURAS NEWLINE structDeclaration*
    ;

structDeclaration
    : ESTRUCTURA IDENTIFIER COLON NEWLINE INDENT structMember+ DEDENT
    ;

structMember
    : variableDeclaration NEWLINE
    | structDeclaration
    ;

functionsSection
    : SECCION_FUNCIONES NEWLINE functionDeclaration+
    ;

functionDeclaration
    : DEFINIR IDENTIFIER LPAREN parameterList? RPAREN (ARROW type)? COLON NEWLINE INDENT statement+ DEDENT
    ;

parameterList
    : parameter (COMMA parameter)*
    ;

parameter
    : type IDENTIFIER                          # scalarParameter
    | LBRACK RBRACK type IDENTIFIER            # arrayParameter
    | LBRACE RBRACE IDENTIFIER IDENTIFIER      # structParameter
    ;

type
    : primitiveType
    | IDENTIFIER
    ;

primitiveType
    : ENTERO | CADENA | FLOTANTE | CARACTER | BOOL
    ;

statement
    : variableDeclaration SEMI? NEWLINE   # variableDeclarationStatement
    | ifStatement                         # ifStmt
    | switchStatement                     # switchStmt
    | forStatement                        # forStmt
    | whileStatement                      # whileStmt
    | doWhileStatement                    # doWhileStmt
    | RETORNAR expression? SEMI? NEWLINE  # returnStatement
    | ROMPER SEMI? NEWLINE                # breakStatement
    | CONTINUAR SEMI? NEWLINE             # continueStatement
    | structDeclaration                   # nestedStructStatement
    | expression SEMI? NEWLINE            # expressionStatement
    ;

variableDeclaration
    : type IDENTIFIER (LBRACK INT_LITERAL RBRACK)? (ASSIGN expression)?
    ;

ifStatement
    : SI LPAREN expression RPAREN ENTONCES NEWLINE INDENT statement+ DEDENT
      elseIfClause*
      elseClause?
    ;

elseIfClause
    : SINO LPAREN expression RPAREN ENTONCES NEWLINE INDENT statement+ DEDENT
    ;

elseClause
    : CONTRARIO NEWLINE INDENT statement+ DEDENT
    ;

switchStatement
    : ELEGIR LPAREN expression RPAREN LBRACE NEWLINE caseClause* defaultClause? RBRACE NEWLINE
    | ELEGIR LPAREN expression RPAREN COLON NEWLINE INDENT caseClause* defaultClause? DEDENT
    ;

caseClause
    : CASO expression COLON NEWLINE statement+
    | CASO expression COLON NEWLINE INDENT statement+ DEDENT
    ;

defaultClause
    : SIEMPRE COLON NEWLINE statement+
    | SIEMPRE COLON NEWLINE INDENT statement+ DEDENT
    ;

forStatement
    : PARA LPAREN forInit? SEMI expression? SEMI forUpdate? RPAREN COLON NEWLINE INDENT statement+ DEDENT
    ;

forInit
    : type IDENTIFIER ASSIGN expression
    | expression
    ;

forUpdate
    : expression
    ;

whileStatement
    : MIENTRAS LPAREN expression RPAREN COLON NEWLINE INDENT statement+ DEDENT
    | MIENTRAS LPAREN expression RPAREN HACER NEWLINE INDENT statement+ DEDENT
    ;

doWhileStatement
    : HACER COLON NEWLINE INDENT statement+ DEDENT MIENTRAS LPAREN expression RPAREN NEWLINE
    ;

expression
    : assignment
    ;

assignment
    : logicalOr (ASSIGN assignment)?
    ;

logicalOr
    : logicalAnd (OR logicalAnd)*
    ;

logicalAnd
    : equality (AND equality)*
    ;

equality
    : relational ((EQ | NEQ) relational)*
    ;

relational
    : additive ((LT | GT | LE | GE) additive)*
    ;

additive
    : multiplicative ((PLUS | MINUS) multiplicative)*
    ;

multiplicative
    : unary ((STAR | DIV) unary)*
    ;

unary
    : (PLUS | MINUS | NOT | INC | DEC) unary
    | postfix
    ;

postfix
    : primary postfixOp*
    ;

postfixOp
    : DOT IDENTIFIER
    | LBRACK expression RBRACK
    | LPAREN argumentList? RPAREN
    | INC
    | DEC
    ;

argumentList
    : expression (COMMA expression)*
    ;

primary
    : INT_LITERAL
    | FLOAT_LITERAL
    | CHAR_LITERAL
    | STRING_LITERAL
    | VERDADERO
    | FALSO
    | IDENTIFIER
    | IMPRIMIR LPAREN argumentList? RPAREN
    | LEER LPAREN argumentList? RPAREN
    | LPAREN expression RPAREN
    | LBRACE expression (COMMA expression)* RBRACE
    ;