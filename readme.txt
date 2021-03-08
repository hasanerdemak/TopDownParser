- In this code I wrote a simple recursive-descent top-down parser for EtuLang code.
EBNF description of EtuLang:

<start> -> <if_statement> | <statement>
<if_statement> -> if <logic_expr> <statement>
[else <statement>]
<logic_expr> -> <var> (< | > | = | >= | <=) <var>
<var> -> A|…|Z|a|…|z|
<statement> -> {<var> := <expr>}
<expr> -> <var> ( +|-|*|/|%) <var>

- I assume EtuLang is case sensitive. For instance, "ElSe" is not accepted.

- I used parsing technique that constructs the parse tree from the top and the input is read from left to right
- I have Node and ParseTree inner classes inside my top class.
- They contain all the necessary functions.
- I also used some of my old codes(lex(), is...()) from HW1 with little changes.

- Firstly I create a Hashtable which holds the token and lexeme values.
- I used RandomAccessFile class to read from file, because In some parts 
of the code I had to get my file pointer back.
- I created my parseTree as I read it from the file and traverse my Tree 
every time the non-terminal function is called, and added it to my output string, which I keep static.
Also I gave the Node to non-terminal functions, which holds their value, as a parameter.
- I had to primitively call the lex() function several times to maintain stability in some parts of my code, 
because in some parts I had to get my file pointer back.

- This program prints the parsetree into result1.txt if it reaches the end of the file without error.
- If any error is detected, it only prints "Parsed incorrectly" into result1.txt