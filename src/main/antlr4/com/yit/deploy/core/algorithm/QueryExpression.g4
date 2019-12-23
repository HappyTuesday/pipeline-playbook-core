grammar QueryExpression;

program: expr=expression EOF;

expression
    : term=TERM # term
    | op=NOT expr=expression # not
    | LPAREN expr=expression RPAREN # paren
    | left=expression op=AND right=expression # and
    | left=expression op=OR right=expression # or
    ;

LPAREN : '(' ;
RPAREN : ')' ;
AND: '&';
OR: ':';
NOT: '!';
TERM: [a-zA-Z_\\.*0-9-]+;
WS : [ \t\r\n]+ -> skip;