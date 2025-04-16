grammar HolyJava;

@header {
package pl.edu.pw.ee;
}

programme: (COMMENT|class|function|(statement? SEMICOLON))*
    ;

statement:    ID '[' expr0 ']' '[' expr0 ']' '=' expr0	        #assignmatrix
            | ID '[' expr0 ']' '=' expr0	                    #assignarray
            | ID '=' expr0		                                #assign
            | ID '=' '[' matitem+ ']'                           #matrix
            | ID '=' arr                                        #array
            | PRINT ID   		                                #print
            | READ ID		                                    #read
            | RETURN expr0                                      #return
            | functioncall                                      #voidfuncall
            | classmethodcall                                   #voidmethodcall
            | WHILE '(' loopcondition ')' whilebody             #while
            | IF '(' ifcondition ')' ifbody (ELSE elsebody)?    #ifelse
            | ID '.' ID '=' expr0                               #assignfield
            | ID '=' NEW ID                                     #newobject
            ;

whilebody: block #whiledef
    ;

ifbody: block #ifdef
    ;

elsebody: block #elsedef
    ;

ifcondition: expr0 #ifcond
    ;

loopcondition: expr0 #loopcond
    ;

class: CLASS ID '{' (COMMENT|method|field)* '}' #classdef
    ;

method: function #methoddef
    ;

field: type ID SEMICOLON #fielddef
    ;

function: funheader block #fundef
    ;

funheader: FUN (VOID|type) ID params #fundefheader
    ;

params: '(' (param (',' param)*)? ')' #funparams
    ;

param: type ID #paramdef
    ;

block: '{' (COMMENT|(statement? SEMICOLON))* '}' #statementblock
    ;

classmethodcall: ID '.' ID '(' (expr0 (',' expr0)*)? ')' #methodcall
    ;

functioncall: ID '(' (expr0 (',' expr0)*)? ')' #funcall
    ;

matitem: arr #matrixitem
    ;

arr: '[' arritem (',' arritem)* ']' #arraydef
    ;

arritem: expr0 #arrayitem
    ;

expr0:    exprComp                   #single0
        | expr0 OR exprComp          #or
        ;

exprComp: expr1                      #singleComp
        | expr1 LT expr1             #less
        | expr1 GT expr1             #greater
        | expr1 EQ expr1             #equal
        | expr1 NEQ expr1            #notequal
        ;

expr1:    expr2			        #single1
        | expr1 XOR expr2	    #xor
        ;

expr2:    expr3			        #single2
        | expr2 AND expr3	    #and
        ;

expr3:    expr4			        #single3
        | expr3 ADD expr4	    #add
        | expr3 SUBSTRACT expr4	#sub
        ;

expr4:    expr5			        #single4
        | expr4 MULT expr5	    #mult
        | expr4 DIVIDE expr5	#div
        ;

expr5:    value             #val
        | NEG expr5		    #neg
        | TOFLOAT expr2     #tofloat
        | TOINT expr2		#toint
        | TOLONG expr2		#tolong
        | TODOUBLE expr2	#todouble
        | '(' expr0 ')'		#par
        | functioncall      #nonvoidfuncall
        | classmethodcall   #nonvoidmethodcall
        | ID '.' ID         #readfield
        ;

value:    ID '[' expr0 ']' '[' expr0 ']' #matrixvalue
        | ID '[' expr0 ']'	             #arrayvalue
        | ID                             #id
        | FLOAT			                 #float
        | INT			                 #int
        | LONG			                 #long
        | DOUBLE		                 #double
        | STRING                         #string
        | BOOL                           #bool
        ;

type:     INTTYPE
        | LONGTYPE
        | FLOATTYPE
        | DOUBLETYPE
        | BOOLTYPE
        ;

COMMENT: '/*' .*? '*/' -> channel(HIDDEN)
    ;

NEW: 'new'
    ;

CLASS: 'class'
    ;

FUN: 'fun'
    ;

RETURN: 'return'
    ;

READ: 'read'
    ;

PRINT: 'print'
    ;

WHILE: 'while'
    ;

IF: 'if'
    ;

ELSE: 'else'
    ;

TOINT: '(int)'
    ;

TOLONG: '(long)'
    ;

TOFLOAT: '(float)'
    ;

TODOUBLE: '(double)'
    ;

BOOL: 'true' | 'false'
    ;

INTTYPE: 'int'
    ;

LONGTYPE: 'long'
    ;

FLOATTYPE: 'float'
    ;

DOUBLETYPE: 'double'
    ;

BOOLTYPE: 'bool'
    ;

VOID: 'void'
    ;

ID: ('a'..'z'|'A'..'Z')+
   ;

FLOAT: '0'..'9'+'.''0'..'9'+'f'
    ;

DOUBLE: '0'..'9'+'.''0'..'9'+
    ;

INT: '0'..'9'+'i'
    ;

LONG: '0'..'9'+
    ;

STRING :  '"' ( ~('\\'|'"') )* '"'
    ;

ADD: '+'
    ;

SUBSTRACT: '-'
    ;

DIVIDE: '/'
    ;

MULT: '*'
    ;

AND: '&'
    ;

OR: '|'
    ;

XOR: '^'
    ;

NEG: '!'
    ;

LT: '<'
    ;

GT: '>'
    ;

EQ: '=='
    ;

NEQ: '!='
    ;

SEMICOLON: ';'
    ;

WHITESPACE: (' '|'\t'|'\r'|'\n')+ -> channel(HIDDEN)
    ;
