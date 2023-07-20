grammar SysY;
options {
	language=Java;
}

// 文法规则
compUnit: (decl | funcDef)+;
decl: constDecl | varDecl;
constDecl: CONST bType constDef (COMMA constDef)* SEMI;
bType: INT | FLOAT;
constDef: IDENT (LBRACK constExp RBRACK)* ASSIGN constInitVal;
constInitVal: constExp | LBRACE (constInitVal (COMMA constInitVal)*)? RBRACE;
varDecl: bType varDef (COMMA varDef)* SEMI;
varDef: IDENT ( LBRACK constExp RBRACK)* | IDENT (LBRACK constExp RBRACK)* ASSIGN initVal;
initVal: exp | LBRACE (initVal (COMMA initVal)*)? RBRACE;
funcDef: funcType IDENT LPAREN (funcFParams)? RPAREN block;
funcType: VOID | INT | FLOAT;
funcFParams: funcFParam (COMMA funcFParam)*;
funcFParam: bType IDENT ( LBRACK RBRACK (LBRACK exp RBRACK)*)?;
block: LBRACE (blockItem)* RBRACE;
blockItem: decl | stmt;
stmt: lVal ASSIGN exp SEMI
    | (exp)? SEMI
    | block
    | IF LPAREN cond RPAREN stmt (ELSE stmt)?
    | WHILE LPAREN cond RPAREN stmt
    | BREAK SEMI
    | CONTINUE SEMI
    | RETURN (exp)? SEMI;
exp: addExp;
cond: lOrExp;
lVal: IDENT (LBRACK exp RBRACK)*;
primaryExp: lVal | number | LPAREN exp RPAREN;
number: INT_CONST | FLOAT_CONST;
unaryExp: primaryExp | IDENT LPAREN (funcRParams)? RPAREN | unaryOp unaryExp;
unaryOp: PLUS | MINUS | NOT;
funcRParams: exp (COMMA exp)*;
//消除mulExp的左递归：mulExp: unaryExp | mulExp (MUL | DIV | MOD) unaryExp;
mulExp: unaryExp ((MUL | DIV | MOD) unaryExp)*;
//消除addExp的左递归：addExp: mulExp | addExp (PLUS | MINUS) mulExp;
addExp: mulExp ((PLUS | MINUS) mulExp)*;
//消除relExp的左递归：relExp: addExp | relExp (LT | GT | LE | GE) addExp;
relExp: addExp ((LT | GT | LE | GE) addExp)*;
//消除eqExp的左递归：eqExp: relExp | eqExp (EQ | NE) relExp;
eqExp: relExp ((EQ | NE) relExp)*;
//消除lAndExp的左递归：lAndExp: eqExp | lAndExp AND eqExp;
lAndExp: eqExp (AND eqExp)*;
//消除lOrExp的左递归：lOrExp: lAndExp | lOrExp OR lAndExp;
lOrExp: lAndExp (OR lAndExp)*;
constExp: addExp;

// 词法规则
CONST: 'const';
INT: 'int';
FLOAT: 'float';
VOID: 'void';
IF: 'if';
ELSE: 'else';
WHILE: 'while';
BREAK: 'break';
CONTINUE: 'continue';
RETURN: 'return';
PLUS: '+';
MINUS: '-';
NOT: '!';
MUL: '*';
DIV: '/';
MOD: '%';
ASSIGN: '=';
LT: '<';
GT: '>';
LE: '<=';
GE: '>=';
EQ: '==';
NE: '!=';
AND: '&&';
OR: '||';
LPAREN: '(';
RPAREN: ')';
LBRACE: '{';
RBRACE: '}';
LBRACK: '[';
RBRACK: ']';
COMMA: ',';
SEMI: ';';

//ident 的语法规则
//消除左递归：IDENT: IDENT_NONDIGIT | IDENT IDENT_NONDIGIT | IDENT IDENT_DIGIT;
IDENT: IDENT_NONDIGIT (IDENT_NONDIGIT | DIGIT)*;
IDENT_NONDIGIT: [a-zA-Z_];


//IntConst 的语法规则
INT_CONST: DECIMAL_CONST | OCTAL_CONST | HEX_CONST;
//消除左递归：DECIMAL_CONST: NONZERO_DIGIT | DECIMAL_CONST DIGIT;
DECIMAL_CONST: NONZERO_DIGIT (DIGIT)*;
//消除左递归：OCTAL_CONST: '0' | OCTAL_CONST OCTAL_DIGIT;
OCTAL_CONST: '0' (OCTAL_DIGIT)*;
//消除左递归：HEX_CONST: HEX_PREFIX HEX_DIGIT | HEX_CONST HEX_DIGIT;
HEX_CONST: HEX_PREFIX (HEX_DIGIT)+;
HEX_PREFIX: '0x' | '0X';
NONZERO_DIGIT: '1'..'9';
DIGIT: '0' | NONZERO_DIGIT;
OCTAL_DIGIT: '0'..'7';
HEX_DIGIT: DIGIT | 'a'..'f' | 'A'..'F';

//FloatConst 的语法规则
FLOAT_CONST: DECIMAL_FLOAT_CONST | HEX_FLOAT_CONST;
DECIMAL_FLOAT_CONST: FRACTIONAL_CONST EXPONENT_PART? FLOAT_SUFFIX? | DIGIT_SEQUENCE EXPONENT_PART FLOAT_SUFFIX?;
HEX_FLOAT_CONST: HEX_PREFIX HEX_FRACTIONAL_CONST BINARY_EXPONENT_PART FLOAT_SUFFIX? | HEX_PREFIX HEX_DIGIT_SEQUENCE BINARY_EXPONENT_PART FLOAT_SUFFIX?;
FRACTIONAL_CONST: DIGIT_SEQUENCE? '.' DIGIT_SEQUENCE | DIGIT_SEQUENCE '.';
EXPONENT_PART: 'e' SIGN? DIGIT_SEQUENCE | 'E' SIGN? DIGIT_SEQUENCE;
SIGN: '+' | '-';
DIGIT_SEQUENCE: DIGIT (DIGIT)*;
HEX_FRACTIONAL_CONST: HEX_DIGIT_SEQUENCE? '.' HEX_DIGIT_SEQUENCE | HEX_DIGIT_SEQUENCE '.';
BINARY_EXPONENT_PART: 'p' SIGN? DIGIT_SEQUENCE | 'P' SIGN? DIGIT_SEQUENCE;
HEX_DIGIT_SEQUENCE: HEX_DIGIT (HEX_DIGIT)*;
FLOAT_SUFFIX: 'f' | 'F' | 'l' | 'L';