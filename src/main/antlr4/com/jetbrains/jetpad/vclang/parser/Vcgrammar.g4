grammar Vcgrammar;

defs  : def*;

def   : static '\\function' precedence name tele* typeTermOpt where?      # defFunction
      | '\\override' name ('\\as' name)? tele* typeTermOpt where?         # defOverride
      | static '\\data' precedence name tele* (':' expr)? constructor*    # defData
      | static '\\class' ID tele* classFields                             # defClass
      | nsCmd name fieldAcc* ('(' name (',' name)* ')')?                  # defCmd
      ;

static  : '\\static'                      # staticStatic
        | '\\dynamic'                     # dynamicStatic
        |                                 # noStatic
        ;

where : '\\where' def+ ';'?;

renamingClause : name '\\to' name;

classFields : '{' defs '}';

nsCmd : '\\open'                        # openCmd
      | '\\close'                       # closeCmd
      | '\\export'                      # exportCmd
      ;

arrow : '<='                            # arrowLeft
      | '=>'                            # arrowRight
      ;

typeTermOpt : ':' expr                  # withType
            | ':' expr arrow expr       # withTypeAndTerm
            | arrow expr                # withTerm
            ;

constructor : '|' precedence name tele*;

precedence :                            # noPrecedence
           | associativity NUMBER       # withPrecedence
           ;

associativity : '\\infix'               # nonAssoc
              | '\\infixl'              # leftAssoc
              | '\\infixr'              # rightAssoc
              ;

name  : ID                              # nameId
      | '(' BIN_OP ')'                  # nameBinOp
      ;

expr  : binOpLeft* maybeNew atomFieldsAcc argument*         # binOp
      | <assoc=right> expr '->' expr                        # arr
      | '\\Pi' tele+ '->' expr                              # pi
      | '\\Sigma' tele+                                     # sigma
      | '\\lam' tele+ '=>' expr                             # lam
      | '\\let' '|'? letClause ('|' letClause)* '\\in' expr # let
      | elimCase expr clause* ';'?                          # exprElim
      ;

letClause : ID tele* typeAnnotation? arrow expr;

typeAnnotation : ':' expr;

clauseName
      : '_'                             # clauseNoName
      | name tele*                      # clauseNameArgs
      ;

clause : '|' clauseName arrow expr;

elimCase : '\\elim'                     # elim
         | '\\case'                     # case
         ;

binOpLeft : maybeNew atomFieldsAcc argument* infix;

maybeNew :                              # noNew
         | '\\new'                      # withNew
         ;

fieldAcc : '.' name                     # classField
         | '.' NUMBER                   # sigmaField
         ;

infix : BIN_OP                          # infixBinOp
      | '`' name fieldAcc* '`'          # infixId
      ;

atom  : '(' expr (',' expr)* ')'        # tuple
      | literal                         # atomLiteral
      | NUMBER                          # atomNumber
      ;

atomFieldsAcc : atom fieldAcc* classFields?;

argument : atomFieldsAcc                # argumentExplicit
         | '{' expr '}'                 # argumentImplicit
         ;

literal : UNIVERSE                      # universe
        | TRUNCATED_UNIVERSE            # truncatedUniverse
        | PROP                          # prop
        | SET                           # set
        | name                          # id
        | '_'                           # unknown
        | '{?}'                         # hole
        ;

tele : literal                          # teleLiteral
     | '(' typedExpr ')'                # explicit
     | '{' typedExpr '}'                # implicit
     ;

typedExpr : expr                        # notTyped
          | expr ':' expr               # typed
          ;

NUMBER : [0-9]+;
UNIVERSE : '\\Type' [0-9]+;
TRUNCATED_UNIVERSE : '\\' [0-9]+ '-Type' [0-9]+;
PROP : '\\Prop';
SET : '\\Set' [0-9]+;
ID : [a-zA-Z_][a-zA-Z0-9_\-\']*;
WS : [ \t\r\n]+ -> skip;
LINE_COMMENT : '--' .*? '\r'? '\n' -> skip;
COMMENT : '{-' .*? '-}' -> skip;
COLON : ':';
LAMBDA : '\\lam';
ARROW : '->';
BIN_OP : [~!@#$%^&*\-+=<>?/:|.]+;
