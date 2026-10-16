parser grammar PigParser;

options {
    tokenVocab = PigLexer;
}

program
    : importDecl* variabilesSection? maiorSection EOF
    ;

importDecl
    : IMPORT (IDENTIFIER DOT)+ IDENTIFIER
    ;

variabilesSection
    : VARIABILES_SECTION variableDecl*
    ;

variableDecl
    : basicVarDecl
    | arrayDecl
    | structInstanceDecl
    | objectInstanceDecl
    ;

basicVarDecl
    : ESTO IDENTIFIER COLON (tipoBasico expression? | expression) SEMI?
    ;

tipoBasico
    : NUMERUS
    | TEXTUM
    | DECIMALIS
    | LITTERA
    | BOOL
    ;

arrayDecl
    : SERIES IDENTIFIER LBRACK INT_LITERAL RBRACK COLON tipoBasico
      (LBRACE expression (COMMA expression)* RBRACE)? SEMI
    ;

structInstanceDecl
    : ESTO IDENTIFIER COLON IDENTIFIER (LBRACE expression (COMMA expression)* RBRACE)? SEMI?
    ;

objectInstanceDecl
    : ESTO IDENTIFIER COLON NOVUS IDENTIFIER LPAREN argumentList? RPAREN SEMI
    ;

maiorSection
    : MAIOR_SECTION statement+ PROGRAM_END SEMI
    ;

statement
    : assignmentStatement
    | readStatement
    | printStatement
    | ifStatement
    | whileStatement
    | doWhileStatement
    | forStatement
    | PERGE SEMI
    | INTERRUMPE SEMI
    | expressionStatement
    ;

assignmentStatement
    : assignmentTarget ASSIGN expression SEMI?
    ;

assignmentTarget
    : IDENTIFIER
    | postfixExpression
    ;

readStatement
    : IDENTIFIER READ_OP
    ;

printStatement
    : PRINT_OP expression (PRINT_OP expression)* SEMI?
    ;

ifStatement
    : SI LPAREN expression RPAREN LBRACE statement* RBRACE
      (ALITER (LPAREN expression RPAREN)? LBRACE statement* RBRACE)*
      FINIS SEMI
    ;

whileStatement
    : DUM LPAREN expression RPAREN LBRACE statement* RBRACE FINIS SEMI
    ;

doWhileStatement
    : FACERE LBRACE statement* RBRACE DUM LPAREN expression RPAREN SEMI
    ;

forStatement
    : PER LPAREN forInit? SEMI expression? SEMI expression? RPAREN LBRACE statement* RBRACE
    ;

forInit
    : ESTO IDENTIFIER COLON tipoBasico expression
    | expression
    ;

expressionStatement
    : postfixExpression SEMI?
    ;

argumentList
    : expression (COMMA expression)*
    ;

expression
    : assignmentExpr
    ;

assignmentExpr
    : logicalOrExpr (ASSIGN assignmentExpr)?
    ;

logicalOrExpr
    : logicalAndExpr (OR logicalAndExpr)*
    ;

logicalAndExpr
    : equalityExpr (AND equalityExpr)*
    ;

equalityExpr
    : relationalExpr ((EQ | NEQ) relationalExpr)*
    ;

relationalExpr
    : additiveExpr ((LT | GT | LE | GE) additiveExpr)*
    ;

additiveExpr
    : multiplicativeExpr ((PLUS | MINUS) multiplicativeExpr)*
    ;

multiplicativeExpr
    : unaryExpr ((STAR | DIV) unaryExpr)*
    ;

unaryExpr
    : (NOT | MINUS | PLUS) unaryExpr
    | postfixExpression
    ;

postfixExpression
    : primaryExpression postfixOp*
    ;

postfixOp
    : DOT IDENTIFIER
    | LBRACK expression RBRACK
    | LPAREN argumentList? RPAREN
    | INC
    | DEC
    ;

primaryExpression
    : INT_LITERAL
    | FLOAT_LITERAL
    | CHAR_LITERAL
    | STRING_LITERAL
    | VERUM
    | FALSUS
    | IDENTIFIER
    | LPAREN expression RPAREN
    | NOVUS IDENTIFIER LPAREN argumentList? RPAREN
    | LBRACE expression (COMMA expression)* RBRACE
    ;