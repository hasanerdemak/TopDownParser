/*
simple recursive-descent top-down parser for EtuLang code
I assume EtuLang is case sensitive. For instance, "ElSe" is not accepted.
EBNF description of EtuLang:

<start> -> <if_statement> | <statement>
<if_statement> -> if <logic_expr> <statement>
[else <statement>]
<logic_expr> -> <var> (< | > | = | >= | <=) <var>
<var> -> A|…|Z|a|…|z|
<statement> -> {<var> := <expr>}
<expr> -> <var> ( +|-|*|/|%) <var>
 */

import java.io.*;
import java.util.ArrayList;
import java.util.Hashtable;

public class hw2_hasanerdem_ak {
    static Hashtable<String, String> table;
    static RandomAccessFile inFile;
    static File outFile;
    static PrintWriter pw;
    static ParseTree parseTree;
    static Node root;

    static String nextToken;
    static String outputString = "";

    public static class Node {
        private String value;
        private Node parent;
        private final ArrayList<Node> children;

        public Node() {
            children = new ArrayList<>();
            this.parent = null;
            this.value = null;
        }

        public Node(Node parent, String value) {
            children = new ArrayList<>();
            this.parent = parent;
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public Node getParent() {
            return parent;
        }

        public void setParent(Node parent) {
            this.parent = parent;
        }

        public Node getChild(int index) {
            return children.get(index);
        }

        public void addChild(String value) {
            children.add(new Node(this, value));
        }

        public void setValue(String value) {
            this.value = value;
        }

        public boolean isEmpty() {
            return value.isEmpty();
        }

        public boolean isLeaf() {
            return children.isEmpty();
        }

        public void traverse() {
            if (outputString != null) {
                if (isLeaf())
                    outputString = outputString + value;
                for (Node node : children)
                    node.traverse();
            }
        }
    }

    public static class ParseTree {
        private final Node root;

        public ParseTree() {
            root = new Node();
        }

        public Node getRoot() {
            return root;
        }

        public boolean isEmpty() {
            return root.isEmpty();
        }

        public void traverse() {
            if(!isEmpty())
                root.traverse();
        }
    }

    public static void main(String[] args) throws Exception {
        //Create a hash table to hold token and lexeme values
        table = new Hashtable<>();
        {
            table.put("=", "EQUAL");
            table.put(":=", "ASSIGNM");
            table.put("+", "ADD");
            table.put("-", "SUBT");
            table.put("/", "DIV");
            table.put("*", "MULT");
            table.put("%", "MODULUS");
            table.put(">", "GREATER");
            table.put("<", "LESS");
            table.put(">=", "GRE_EQ");
            table.put("<=", "LESS_EQ");
            table.put(":", "COLON");
            table.put("{", "L_CURLYBRACKET");
            table.put("}", "R_CURLYBRACKET");
            table.put("if", "RES_WORD");
            table.put("else", "RES_WORD");
        }

        inFile = new RandomAccessFile(args[0], "r"); //"sample1.ETU"
        outFile = new File(args[1]); //"result1.txt"
        pw = new PrintWriter(new FileWriter(outFile));

        parseTree = new ParseTree();
        root = parseTree.getRoot();
        root.setValue("<start>");
        START(root);
        end();
    }

    // <start> -> <if_statement> | <statement>
    public static void START(Node startNode) throws Exception {
        parseTree.traverse();
        outputString = outputString + "\n";
        lex();
        if (nextToken.equals("if")) {
            startNode.addChild(" <if_statement> ");
            IF_STATEMENT(startNode.getChild(0));
        }
        else if (nextToken.equals("{")) {
            inFile.seek(0);
            startNode.addChild(" <statement> ");
            STATEMENT(startNode.getChild(0));
        }
        else{
            outputString = null; // If any error is detected set the outputString null and call the end() function
            end();
            //throw new Exception("START Exception");
        }
    }

    // <if_statement> -> if <logic_expr> <statement> [else <statement>]
    public static void IF_STATEMENT(Node ifStmtNode) throws Exception {
        boolean isContainElse = false;
        while (inFile.getFilePointer() < inFile.length()-1) { //Look ahead and check if there is any else
            lex();
            if (nextToken.equals("else")) {
                isContainElse = true;
                break;
            }
        }
        inFile.seek(0); //reset pointer to first
        lex();
        parseTree.traverse();
        outputString = outputString + "\n";
        if (nextToken.equals("if")){ //add nodes to parseTree
            ifStmtNode.addChild(" "+nextToken+" ");
            ifStmtNode.addChild(" <logic_expr> ");
            ifStmtNode.addChild(" <statement> ");
            if (isContainElse) {
                ifStmtNode.addChild(" " + "else" + " ");
                ifStmtNode.addChild(" <statement> ");
            }
            LOGIC_EXPR(ifStmtNode.getChild(1)); //call the leftmost non-terminal function
            STATEMENT(ifStmtNode.getChild(2));
            lex();
            lex();
            if (isContainElse) {
                if (nextToken.equals("else")) {
                    STATEMENT(ifStmtNode.getChild(4));
                } else {
                    outputString = null;
                    end();
                    //throw new Exception("IF_STATEMENT Exception1(else)");
                }
            }
        } else{
            outputString = null;
            end();
            //throw new Exception("IF_STATEMENT Exception2");
        }
    }

    // <logic_expr> -> <var> (< | > | = | >= | <=) <var>
    public static void LOGIC_EXPR(Node logicExprNode) throws Exception {
        long a = inFile.getFilePointer(); // Remember where the pointer is before checking the operator
        parseTree.traverse();
        outputString = outputString + "\n";
        logicExprNode.addChild(" <var> ");
        lex();
        lex();
        if ( nextToken.equals("<") || nextToken.equals(">") || nextToken.equals("=") || nextToken.equals(">=") || nextToken.equals("<=") ) {
            logicExprNode.addChild(" "+nextToken+" ");
            logicExprNode.addChild(" <var> ");
            inFile.seek(a);
            lex();
            VAR(logicExprNode.getChild(0));
            lex();
            lex();
            VAR(logicExprNode.getChild(2));
        } else{
            outputString = null;
            end();
            //throw new Exception("LOGIC_EXPR Exception");
        }
    }

    // <var> -> A|…|Z|a|…|z|
    public static void VAR(Node varNode) throws Exception {
        parseTree.traverse();
        outputString = outputString + "\n";
        if (!nextToken.isEmpty()) {
            if (nextToken.length() == 1) {
                if (isVar(nextToken.charAt(0))) {
                    varNode.addChild(" " + nextToken + " ");
                } else {
                    outputString = null;
                    end();
                    //throw new Exception("VAR Exception1");
                }
            } else {
                outputString = null;
                end();
                //throw new Exception("VAR Exception2");
            }
        }else{
            outputString = null;
            end();
            //throw new Exception("VAR Exception3");
        }
    }

    //<statement> -> {<var> := <expr>}
    public static void STATEMENT(Node stmtNode) throws Exception {
        long a = inFile.getFilePointer();
        parseTree.traverse();
        outputString = outputString + "\n";
        lex();
        if (nextToken.equals("{")) {
            stmtNode.addChild(" "+nextToken+" ");
            stmtNode.addChild(" <var> ");
            lex();
            lex();
            if (nextToken.equals(":=")){
                stmtNode.addChild(" "+nextToken+" ");
                stmtNode.addChild(" <expr> ");
                lex();
                lex();
                lex();
                lex();
                if (nextToken.equals("}")) {
                    stmtNode.addChild(" "+nextToken+" ");
                    inFile.seek(a);
                    lex();
                    lex();
                    VAR(stmtNode.getChild(1));
                    lex();
                    EXPR(stmtNode.getChild(3));
                } else{
                    outputString = null;
                    end();
                    //throw new Exception("STATEMENT Exception1");
                }
            } else{
                outputString = null;
                end();
                //throw new Exception("STATEMENT Exception2");
            }
        } else{
            outputString = null;
            end();
            //throw new Exception("STATEMENT Exception3");
        }
    }

    //<expr> -> <var> ( +|-|*|/|%) <var>
    public static void EXPR(Node exprNode) throws Exception {
        long a = inFile.getFilePointer();
        parseTree.traverse();
        outputString = outputString + "\n";
        exprNode.addChild(" <var> ");
        lex();
        lex();
        if ( nextToken.equals("+") || nextToken.equals("-") || nextToken.equals("*") || nextToken.equals("/") || nextToken.equals("%") ) {
            exprNode.addChild(" "+nextToken+" ");
            exprNode.addChild(" <var> ");
            inFile.seek(a);
            lex();
            VAR(exprNode.getChild(0));
            lex();
            lex();
            VAR(exprNode.getChild(2));
        } else{
            outputString = null;
            end();
            //throw new Exception("EXPR Exception");
        }
    }

    // prints the result
    public static void end() throws IOException {
        // Check if there is anything other than '}', including a different '}'
        if (!nextToken.equals("}")) {
            lex();
            while (inFile.getFilePointer() < inFile.length()-1 ) {
                if (nextToken.equals("}")) {
                    long a = inFile.getFilePointer();
                    long b = inFile.getFilePointer();
                    while (inFile.getFilePointer() < inFile.length()) {
                        if (nextToken.equals("}") && a != b) { // if different '}' is detected, then set the outputString null
                            outputString = null;
                            break;
                        }
                        lex();
                        b = inFile.getFilePointer(); //Assign pointers location to b again to check if the pointer is moved.
                    }
                }
                lex();
            }
        }
        lex();
        if (nextToken.equals("}")) {
            parseTree.traverse();
            if (outputString != null) {
                //Reformat the outputString
                outputString = outputString.replaceAll(" {2}", " ");
                outputString = outputString.replaceAll("\n ", "\n");
                outputString = outputString.replaceAll("\\{ ", "{");
                outputString = outputString.replaceAll(" }", "}");
                pw.write("Parsed correctly\n" +
                            "ParseTree :\n" +
                            outputString);
            } else {
                pw.write("Parsed incorrectly");
            }
        } else {
            pw.write("Parsed incorrectly");
        }
        pw.close();
        System.exit(0);
    }

    // finds the next lexeme
    public static void lex() throws IOException {
        String str = "";
        int a;

        while (inFile.getFilePointer() < inFile.length()) {
            a = inFile.read();
            if (table.containsKey("" + (char) a)) { // If the character is in table, then control if it is in the table with the next character
                if (!str.isEmpty()) { // before do it, check if str has any character
                    nextToken = str;
                    inFile.seek(inFile.getFilePointer()-1);
                    return;
                }
                str = (a > 32) ? "" + (char) a : ""; // add "a" to str if "a" is not a space or an escape character
                a = inFile.read();
                if (table.containsKey("" + (char) a) || a == '=') { // Control equality operator separately because it is not included in the table alone
                    if (!table.containsKey(str + (char) a)) {
                        inFile.seek(inFile.getFilePointer()-1);
                        nextToken = str;
                        return;
                    } else {
                        str = (a > 32) ? str + (char) a : str;
                        nextToken = str;
                        return;
                    }
                } else {
                    inFile.seek(inFile.getFilePointer()-1);
                    nextToken = str;
                    return;
                }
            } else if (!(isAlphaNum((char) a) || isUnknownToken((char) a))) { // Check if lexeme is over. (The "if"s above also check this)
                if (!str.isEmpty()) {
                    nextToken = str;
                    return;
                }
            } else str = (a > 32) ? str + (char) a : str; // Else, add "a" to str
        }
        if (!str.isEmpty()) {
            nextToken = str;
        }

    }

    // Checks if character is alphanum
    public static boolean isAlphaNum(char a) {
        return (a >= 'a' && a <= 'z') || (a >= 'A' && a <= 'Z') || (a >= '0' && a <= '9');
    }

    // Checks if character is variable that matches the language description
    public static boolean isVar(char a){
        return (a >= 'a' && a <= 'z') || (a >= 'A' && a <= 'Z');
    }

    // Checks if character is unknown token which is not in table
    public static boolean isUnknownToken(char a) {
        return !table.containsKey("" + a) && a > 32 && !isAlphaNum(a);
    }
}
