parser grammar ZetarianoParser;

options {
    tokenVocab = ZetarianoLexer;
}

classDeclaration
    : PUBLIC? CLASS IDENTIFIER (EXTENDS IDENTIFIER)? LBRACE classMember* RBRACE EOF
    ;

classMember
    : fieldDeclaration
    | constructorDeclaration
    | methodDeclaration
    ;

// No modifier means default (package) access.
visibilityModifier
    : PUBLIC
    | PRIVATE
    | PROTECTED
    ;

type
    : (INT | DOUBLE | CHAR | BOOLEAN | STRING | IDENTIFIER) (LBRACKET RBRACKET)*
    ;

fieldDeclaration
    : visibilityModifier? type IDENTIFIER SEMI
    ;

constructorDeclaration
    : visibilityModifier? IDENTIFIER LPAREN parameterList? RPAREN block
    ;

methodDeclaration
    : OVERRIDE? visibilityModifier? (type | VOID) IDENTIFIER LPAREN parameterList? RPAREN block
    ;

parameterList
    : parameter (COMMA parameter)*
    ;

parameter
    : type IDENTIFIER
    ;

block
    : LBRACE statement* RBRACE
    ;

statement
    : variableDeclarationStatement
    | ifStatement
    | whileStatement
    | doWhileStatement
    | forStatement
    | switchStatement
    | returnStatement
    | printlnStatement
    | printStatement
    | breakStatement
    | continueStatement
    | block
    | expressionStatement
    ;

variableDeclarationStatement
    : type IDENTIFIER (ASSIGN variableInitializer)? SEMI
    ;

variableInitializer
    : expression
    | arrayInitializer
    ;

// String[] names = {"a", "b"}; int[][] m = {{1, 2}, {3, 4}};
arrayInitializer
    : LBRACE (variableInitializer (COMMA variableInitializer)*)? RBRACE
    ;

expressionStatement
    : expression SEMI
    ;

statementOrBlock
    : block
    | statement
    ;

ifStatement
    : IF LPAREN expression RPAREN statementOrBlock (ELSE statementOrBlock)?
    ;

whileStatement
    : WHILE LPAREN expression RPAREN statementOrBlock
    ;

doWhileStatement
    : DO statementOrBlock WHILE LPAREN expression RPAREN SEMI
    ;

forStatement
    : FOR LPAREN forInit? SEMI expression? SEMI forUpdate? RPAREN statementOrBlock
    ;

forInit
    : type IDENTIFIER (ASSIGN expression)?
    | expression
    ;

forUpdate
    : expression (COMMA expression)*
    ;

switchStatement
    : SWITCH LPAREN expression RPAREN LBRACE switchLabel* RBRACE
    ;

switchLabel
    : CASE (INT_LITERAL | CHAR_LITERAL | STRING_LITERAL | TRUE | FALSE) COLON statement*
    | DEFAULT COLON statement*
    ;

returnStatement
    : RETURN expression? SEMI
    ;

printlnStatement
    : PRINTLN LPAREN argumentList? RPAREN SEMI
    ;

printStatement
    : PRINT LPAREN argumentList? RPAREN SEMI
    ;

breakStatement
    : BREAK SEMI
    ;

continueStatement
    : CONTINUE SEMI
    ;


expression
    : assignmentExpression
    ;

assignmentExpression
    : ternaryExpression ((ASSIGN | PLUS_ASSIGN | MINUS_ASSIGN | MUL_ASSIGN) assignmentExpression)?
    ;

ternaryExpression
    : logicalOrExpression (QUESTION expression COLON expression)?
    ;

logicalOrExpression
    : logicalAndExpression (OR logicalAndExpression)*
    ;

logicalAndExpression
    : equalityExpression (AND equalityExpression)*
    ;

equalityExpression
    : relationalExpression ((EQ | NEQ) relationalExpression)*
    ;

relationalExpression
    : additiveExpression ((LT | GT | LE | GE) additiveExpression)*
    ;

additiveExpression
    : multiplicativeExpression ((PLUS | MINUS) multiplicativeExpression)*
    ;

multiplicativeExpression
    : unaryExpression ((STAR | DIV | MOD) unaryExpression)*
    ;

unaryExpression
    : (PLUS | MINUS | NOT | PLUS_PLUS | MINUS_MINUS) unaryExpression
    | LPAREN castType RPAREN unaryExpression
    | postfixExpression
    ;

// (int) x, (double) x, (char) x: conversions between the numeric types
castType
    : INT | DOUBLE | CHAR
    ;

postfixExpression
    : primaryExpression postfixOp*
    ;

postfixOp
    : PLUS_PLUS
    | MINUS_MINUS
    | DOT IDENTIFIER
    | LBRACKET expression RBRACKET
    | LPAREN argumentList? RPAREN
    ;

primaryExpression
    : INT_LITERAL
    | DOUBLE_LITERAL
    | CHAR_LITERAL
    | STRING_LITERAL
    | TRUE
    | FALSE
    | NULL
    | THIS
    | SUPER
    | READLN LPAREN RPAREN
    | IDENTIFIER
    | arrayCreation
    | NEW IDENTIFIER LPAREN argumentList? RPAREN
    | LPAREN expression RPAREN
    ;

arrayCreation
    : NEW (INT | DOUBLE | CHAR | BOOLEAN | STRING | IDENTIFIER) (LBRACKET expression RBRACKET)+
    ;

argumentList
    : expression (COMMA expression)*
    ;