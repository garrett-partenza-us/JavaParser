package p1;

import java.util.*;
import java.util.logging.Logger;

/*
 * GRAMMAR FOR PROCESSING SIMPLE SENTENCES:
 *
 * <SENTENCE> ::= <NP> <VP> <NP> <PP> <SENTENCE_TAIL>
 * <SENTENCE_TAIL> ::= <CONJ> <SENTENCE> <EOS> | <EOS>
 *
 * <NP> ::= <ART> <ADJ_LIST> <NOUN>
 * <ADJ_LIST> ::= <ADJ> <ADJ_TAIL> | <<EMPTY>>
 * <ADJ_TAIL> ::= <COMMA> <ADJ> <ADJ_TAIL> | <<EMPTY>>
 *
 * <VP> ::= <ADV> <VERB> | <VERB>
 * <PP> ::= <PREP> <NP> | <<EMPTY>>
 *
 * // *** Terminal Productions (Actual terminals omitted, but they are just the
 * valid
 * words in the language). ***
 * <COMMA> ::= ','
 * <EOS> ::= '.' | '!'
 * <ADJ> ::= ...adjective list...
 * <ADV> ::= ...adverb list...
 * <ART> ::= ...article list...
 * <CONJ> ::= ...conjunction list...
 * <NOUN> ::= ...noun list...
 * <PREP> ::= ...preposition list...
 * <VERB> ::= ...verb list....
 */
/**
 * The Syntax Analyzer
 */

class Parser {

// The lexer which will provide the tokens
    private final LexicalAnalyzer lexer;

    // the actual "code generator"
    private final CodeGenerator codeGenerator;

    /**
     * The constructor initializes the terminal literals in their vectors.
     *
     * @param lexer The Lexer Object
     */
    public Parser(LexicalAnalyzer lexer, CodeGenerator codeGenerator) {
        this.lexer = lexer;
        this.codeGenerator = codeGenerator;
    }

    /**
     * Begin analyzing...
     *
     * @throws MockCompiler.ParseException
     */
    public void analyze() {
        try {
            // Generate header for our output
            var startNode = codeGenerator.buildNode("PARSE TREE");
            codeGenerator.writeHeader(startNode);

            // Start the actual parsing.   
            program(startNode);

            // generate footer for our output
            codeGenerator.writeFooter();

            // For graphically displaying the output.
            // CodeGenerator.openWebGraphViz();
        } catch (ParseException ex) {
            final String msg = String.format("%s\n", ex.getMessage());
            Logger.getAnonymousLogger().severe(msg);
        }
    }

    // <program> ::= <stmt_list>
    protected void program(ParseNode fromNode) throws ParseException {
        final var nodeName = codeGenerator.addNonTerminalToTree("<program>", fromNode);
        
        stmt_list(nodeName);
        
    }

    //<stmt_list> ::= <stmt> <stmnt_list> | e
    void stmt_list(ParseNode fromNode) throws ParseException {
        final var treeNode = codeGenerator.addNonTerminalToTree("<stmt_list>", fromNode);

        if (lexer.isCurrentToken(TOKEN.ID) || lexer.isCurrentToken(TOKEN._if) || lexer.isCurrentToken(TOKEN._while) || lexer.isCurrentToken(TOKEN.read) || lexer.isCurrentToken(TOKEN.write)) {
            stmt(treeNode);
            stmt_list(treeNode);
        }else{
            EMPTY(treeNode);
        }
    
    }

    // <stmt> ::= <id> <:=> <expr> | <read> <ID> | <write> <expr>
    void stmt(ParseNode fromNode) throws ParseException {
        final var  treeNode = codeGenerator.addNonTerminalToTree("<stmt>", fromNode);

        if(lexer.isCurrentToken(TOKEN.closure)){
            processTerminal(treeNode);
        }else if (lexer.isCurrentToken(TOKEN.read)) {
            processTerminal(treeNode);
            processTerminal(treeNode);
        } else if (lexer.isCurrentToken(TOKEN.write)) {
            processTerminal(treeNode);
            expr(treeNode);
        } else if (lexer.isCurrentToken(TOKEN._if)) {
            processTerminal(treeNode);
            condition(treeNode);
            processTerminal(treeNode);
            stmt_list(treeNode);
            processTerminal(treeNode);
        } else if (lexer.isCurrentToken(TOKEN._while)) {
            processTerminal(treeNode);
            condition(treeNode);
            processTerminal(treeNode);
            stmt_list(treeNode);
            processTerminal(treeNode);
        } else{
            processTerminal(treeNode);
            processTerminal(treeNode);
            expr(treeNode);
        }
    }

    //expr ::= <term> <term_tail>
    void expr(ParseNode fromNode) throws ParseException {
        final var treeNode = codeGenerator.addNonTerminalToTree("<expr>", fromNode);

        term(treeNode);
        term_tail(treeNode);
    }

    // term_tail ::= <add_op> <term> <term_tail>
    void term_tail(ParseNode fromNode) throws ParseException {
        final var treeNode = codeGenerator.addNonTerminalToTree("<term_tail>", fromNode);

        if (lexer.isCurrentToken(TOKEN.add_op)) {
            add_op(treeNode);
            term(treeNode);
            term_tail(treeNode);

        }
        else{
            EMPTY(treeNode);
        }

    }

    // term ::= <factor> <factor_tail>
    void term(ParseNode fromNode) throws ParseException {
        final var treeNode = codeGenerator.addNonTerminalToTree("<term>", fromNode);

        factor(treeNode);
        factor_tail(treeNode);
    }
    
    // factor_tail ::= <mult_op> <factor> <factor_tail> | e
    void factor_tail(ParseNode fromNode) throws ParseException {
        final var treeNode = codeGenerator.addNonTerminalToTree("<factor_tail>", fromNode);

        if (lexer.isCurrentToken(TOKEN.mult_op)) {
            mult_op(treeNode);
            factor(treeNode);
            factor_tail(treeNode);
        }
        else{
            EMPTY(treeNode);
        }
        
    }
    
    // factor ::= <( expr )> | <id> | number
    void factor(ParseNode fromNode) throws ParseException {
        final var treeNode = codeGenerator.addNonTerminalToTree("<factor>", fromNode);

        if (lexer.isCurrentToken(TOKEN.parenthesis)) {
            processTerminal(treeNode);
            expr(treeNode);
            processTerminal(treeNode);
        }
        else{
            processTerminal(treeNode);
        }
        
    }
    
    // condition ::= expr relation expr
    void condition(ParseNode fromNode) throws ParseException {
        final var treeNode = codeGenerator.addNonTerminalToTree("<condition>", fromNode);

        expr(treeNode);
        relation(treeNode);
        expr(treeNode);
        
    }
    
    // relation = ...
    void relation(ParseNode fromNode) throws ParseException {
        final var treeNode = codeGenerator.addNonTerminalToTree("<relation>", fromNode);

        processTerminal(treeNode);
        
    }
    
    //add_op ::= <+> | <->
    void add_op(ParseNode fromNode) throws ParseException {
        if (!lexer.isCurrentToken(TOKEN.add_op)) {
            raiseException("an addition operator", fromNode);
        } else {
            processTerminal(fromNode);
        }
    }
    
    //mult_op ::= <*> | </>
    void mult_op(ParseNode fromNode) throws ParseException {
        if (!lexer.isCurrentToken(TOKEN.mult_op)) {
            raiseException("an multiplication operator", fromNode);
        } else {
            processTerminal(fromNode);
        }
    }
    
    //number ::= <0-9>
    void number(ParseNode fromNode) throws ParseException {
        if (!lexer.isCurrentToken(TOKEN.number)) {
            raiseException("an number", fromNode);
        } else {
            processTerminal(fromNode);
        }
    }
    
    
    //read ::= <read>
    void read(ParseNode fromNode) throws ParseException {
        if (!lexer.isCurrentToken(TOKEN.read)) {
            raiseException("an read", fromNode);
        } else {
            processTerminal(fromNode);
        }
    }
    
    //write ::= <write>
      void write(ParseNode fromNode) throws ParseException {
          if (!lexer.isCurrentToken(TOKEN.write)) {
              raiseException("an write", fromNode);
          } else {
              processTerminal(fromNode);
          }
      }
    
    //parenthesis ::= <parenthesis>
      void parenthesis(ParseNode fromNode) throws ParseException {
          if (!lexer.isCurrentToken(TOKEN.parenthesis)) {
              raiseException("an parenthesis", fromNode);
          } else {
              processTerminal(fromNode);
          }
      }
    
    
    //EOS ::= <e>
      void EOS(ParseNode fromNode) throws ParseException {
          if (!lexer.isCurrentToken(TOKEN.EOS)) {
              raiseException("an EOS token", fromNode);
          } else {
              processTerminal(fromNode);
          }
      }
  
    
    /////////////////////////////////////////////////////////////////////////////////////
    // For the sake of completeness, each terminal-token has it's own method,
    // though they all do the same thing here.  In a "REAL" program, each terminal
    // would likely have unique code associated with it.
    /////////////////////////////////////////////////////////////////////////////////////
    private void EMPTY(ParseNode fromNode) throws ParseException {
        codeGenerator.addEmptyToTree(fromNode);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Terminal:
    // Test it's type and continute if we really have a terminal node, syntax error if fails.
    void processTerminal(ParseNode fromNode) throws ParseException {
        final var currentTerminal = lexer.getCurrentToken();
        final var nodeLable = String.format("<%s>", currentTerminal);
        final var terminalNode = codeGenerator.buildNode(nodeLable);

        codeGenerator.addNonTerminalToTree(fromNode, terminalNode);
        codeGenerator.addTerminalToTree(terminalNode, lexer.getLexeme());
        lexer.advanceToken();

    }

// The code below this point is just a bunch of "helper functions" to keep the
// parser code (above) a bit cleaner.
// Handle all of the errors in one place for cleaner parser code.
    private void raiseException(String expected, ParseNode fromNode) throws ParseException {
        final var template = "SYNTAX ERROR: '%s' was expected but '%s' was found.";
        final var err = String.format(template, expected, lexer.getLexeme());
        codeGenerator.syntaxError(err, fromNode);

    }

    static class ParseException extends Exception {

        public ParseException(String errMsg) {
            super(errMsg);
        }
    }

}

/**
 * All Of the Tokens/Terminals Used by the parser.
 * The purpose of the enumm type here is eliminate the need for direct
 * string comparisons which is generally slow, as being difficult to maintain.
 * (We want Java's "static type checking" to do as much work for us as it can!)
 *
 * !!!!! IMPORTANT !!!!
 * -----------------------------------------------------------------
 * IN MOST CASES REAL "PROGRAMMING LANGUAGE" CASES
 * THERE WILL BE ONLY ONE LEXEME PER TOKEN !!!
 * -----------------------------------------------------------------
 * !!!!! IMPORTANT !!!!
 *
 * The fact that several lexemes exist per token in this example is because this
 * is to parse simple English sentences where most of the token types have many
 * words (lexemes) that could fit. This is generally NOT the case in most
 * programming
 * languages!!!
 */
enum TOKEN {
    add_op("+", "-"),
    mult_op("*", "/"),
    parenthesis("(", ")"),
    EOS("e", "EOF"),
    read("read"),
    write("write"),
    _if("if"),
    closure("fi", "od"),
    _while("while"),
    EOF(), // End of file
    OTHER("other"),
    ID(),
    number("1","2","3","4","5","6","7","8","9","0");

    private final List<String> lexemeList;

    private TOKEN(String... tokenStrings) {
        lexemeList = new ArrayList<>(tokenStrings.length);
        lexemeList.addAll(Arrays.asList(tokenStrings));
    }

    public static TOKEN fromLexeme(final String string) {
        // Just to be safe...
        var lexeme = string.trim();

        // An empty string should mean no more tokens to process.
        if (lexeme.isEmpty()) {
            return EOF;
        }


        // digits only (doesn't handle "-" or ".", only digist)
        if (lexeme.matches("\\d+")) {
            return number;
        }

        // Search through ALL lexemes looking for a match with early bailout.
        for (var t : TOKEN.values()) {
            if (t.lexemeList.contains(lexeme)) {
                // early bailout.
                return t;
            }
        }

        // NOTE: Other could represent a number, for example.
        return ID;
    }
}
