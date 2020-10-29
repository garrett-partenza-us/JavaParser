package p1;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;

import static java.lang.System.err;
import static java.lang.System.out;

/**
 * COSC 455 Programming Languages: Implementation and Design.
 *
 * DESIGN NOTE: It's generally bad to have a bunch of "top level classes" in one
 * giant file. However, this was done here only to keep the example down to only
 * files... One for the p1; and one for everything else!
 *
 * This syntax analyzer implements a top-down, left-to-right, recursive-descent
 * parser based on the production rules for a simple English language provided
 * by Weber in "Modern Programming Languages".
 */
public class MAIN_Compiler {

    // toggle to display grapviz prompt.
    static final boolean PROMPT_FOR_GRAPHVIZ = false;

    public static void main(String[] args) throws Parser.ParseException {
        // Check for an input file argument
        if (args.length == 1) {
            final File file = new File(args[0]);
            if (file.exists()) {
                compile(file);
            } else {
                err.printf("Input file not found: %s%n", file.toPath());
                return;
            }

            // Display the graphviz test page if desired.
            if (PROMPT_FOR_GRAPHVIZ) {
                CodeGenerator.openWebGraphViz();
            }
        } else {
            err.println("Must Provide an input filename!!");
        }
    }

    /**
     * Start the lexing/parsing/compiling process.
     *
     * @param inputFile the File to read for input.
     */
    static void compile(File inputFile) {
        final CodeGenerator codeGenerator = new CodeGenerator();
        final LexicalAnalyzer lexicalAnalyzer = new LexicalAnalyzer(inputFile);

        // Compile the program from the input supplied by the lexical analyzer.
        out.println(lexicalAnalyzer);
        final Parser parser = new Parser(lexicalAnalyzer, codeGenerator);
        parser.analyze();
    }

}

/**
 * *****************************************************************************
 * This is a *FAKE* (Overly simplified) Lexical Analyzer...
 *
 * NOTE: This DOES NOT "lex" the input in the traditional manner on a DFA based
 * "state machine" or even host language supported regular expressions (which
 * are compiled into "state machine" anyhow in most implementations.)
 *
 * Instead of using "state transitions", this is merely a quick hack to create
 * a something that BEHAVES like a traditional lexer in it's FUNCTIONALITY, but
 * it ONLY knows how to separate (tokenize) lexemes delimited by SPACES. A Real
 * Lexer would tokenize based upon far more sophisticated lexical rules.
 *
 * AGAIN: ALL TOKENS MUST BE WHITESPACE DELIMITED.
 */
class LexicalAnalyzer {

    // TOKENIZED input.
    private Queue<TokenString> tokenList;

    // Simple Wrapper around current token.
    boolean isCurrentToken(TOKEN token) {
        return token == getCurrentToken();
    }

    /**
     * Construct a lexer over an input string.
     *
     * @param inputString
     */
    LexicalAnalyzer(String inputString) {
        tokenize(inputString);
    }

    /**
     * Construct a Lexer over the contents of a file. Filters out lines starting
     * with a '#' Symbol. Removes EOL markers since. (Otherwise, our grammar
     * would have to deal with them).
     *
     * @param inputFile
     */
    LexicalAnalyzer(File inputFile) {
        try {
            Path filePath = inputFile.toPath();

            tokenize(Files.lines(filePath) // read lines
                    .map(String::trim) // map to trimmed strings
                    .filter(x -> !x.startsWith("#")) // filter out lines starting with #
                    .collect(Collectors.joining(" "))); // join lines together with spaces in between. 
        } catch (IOException ex) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "Error Reading File: {0}", ex);
        }
    }

    /*
     * Convert the line to a series of tokens.
     */
    private void tokenize(final String line) {
        // Using Java 8's "Function Streams"
        this.tokenList = Arrays
                .stream(line.trim().split("\\s+")) // split string on spaces
                .map(TokenString::new) // map to a new Token
                .collect(Collectors.toCollection(LinkedList::new)); // collect into a new list
    }

    /**
     * Get just the lexeme part the head of the token list.
     *
     * @return the Lexeme as an Optional string since an empty list has no
     *         tokens.
     */
    public String getLexeme() {
        return (this.tokenList.isEmpty() || getCurrentToken() == TOKEN.EOF)
                ? "EOF"
                : this.tokenList.peek().lexeme;
    }

    /*
     * get just the token
     */
    public TOKEN getCurrentToken() {
        return this.tokenList.isEmpty()
                ? TOKEN.EOF
                : this.tokenList.peek().token;
    }

    /*
     * Advance to next token, making it current.
     */
    public void advanceToken() {
        if (!this.tokenList.isEmpty()) {
            this.tokenList.remove();
        }
    }

    @Override
    public String toString() {
        return this.tokenList.toString();
    }

    /**
     * Nested class: a "Pair Tuple/Struct" for the token type and original
     * string.
     *
     */
    private class TokenString {

        private final String lexeme;
        private final TOKEN token;

        TokenString(String lexeme) {
            this.lexeme = lexeme;
            this.token = TOKEN.fromLexeme(lexeme);
        }

        @Override
        public String toString() {
            var msg = String.format("{lexeme=%s, token=%s}", lexeme, token);
            return msg;
        }
    }
}

/**
 * *****************************************************************************
 * A "3-Tuple" for the node name and id number.
 */
class ParseNode {

    private final String nodeName;
    private final Integer nodeId;
    private static Integer currentNodeID = 0;

    ParseNode(String nodeName) {
        this.nodeName = nodeName;
        this.nodeId = currentNodeID++;
    }

    public String getNodeName() {
        return nodeName;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    @Override
    public String toString() {
        return String.format("%s-%s", this.getNodeName(), this.getNodeId());
    }
}

/**
 * *****************************************************************************
 * This is a ***SIMULATION*** of a "code generator" that simply generates
 * GraphViz output. Technically, this would represent be the "Intermediate Code
 * Generation" step.
 *
 * Also, Instead of building an entire tree in memory followed by a traversal
 * the tree at the end, here were are just adding "code" as we go.
 *
 * (This simulates a single-pass compiler; keep in mind that most modern
 * compilers work in several passes... eg. scan for all top level identifiers,
 * build sub-trees for each class/method/etc., generate an internal
 * intermediate code representation, and so on).
 *
 * DESIGN NOTE: From an OOP design perspective, creating instances of "utility
 * classes" (classes with no internal state) is generally bad. However, in
 * a more elaborate example, the code generator would most certainly maintain
 * some internal state information. (Memory address offsets, etc.)
 */
class CodeGenerator {

    /**
     * Add an "inner node" to the parse tree.
     *
     * @param newNodeName the name of the node to insert into the tree
     * @param parentNode  the parent of the node being added to the tree
     * @return the newly added node as ParseNode object.
     */
    public ParseNode addNonTerminalToTree(String newNodeName, ParseNode parentNode) {
        final var toNode = buildNode(newNodeName);
        addNonTerminalToTree(parentNode, toNode);
        return toNode;
    }

    // Show the terminals as ovals...
    public void addEmptyToTree(ParseNode fromNode) {
        var node = new ParseNode("ENPTY");
        out.printf("\t\"%s\" -> {\"%s\" [label=\"%s\", shape=none]};%n", fromNode, node, "&epsilon;");
    }

    // Show the non-terminals as boxes...
    public void addNonTerminalToTree(ParseNode fromNode, ParseNode toNode) {
        out.printf("\t\"%s\" -> {\"%s\" [label=\"%s\", shape=rect]};%n", fromNode, toNode, toNode.getNodeName());
    }

    // Show the terminals as ovals...
    public void addTerminalToTree(ParseNode fromNode, String lexeme) {
        var node = new ParseNode(lexeme);
        out.printf("\t\"%s\" -> {\"%s\" [label=\"%s\", shape=oval]};%n", fromNode, node, lexeme);
    }

    // Call this if a syntax error occurs...
    public void syntaxError(String err, ParseNode fromNode) throws Parser.ParseException {
        out.printf("\t\"%s\" -> {\"%s\"};%n}%n", fromNode, err);
        throw new Parser.ParseException(err);
    }

    // Build a node name so it can be later "deconstructed" for the output.
    public ParseNode buildNode(String name) {
        return new ParseNode(name);
    }

    // "Real" executable code generally has a header.  See:
    // https://en.wikipedia.org/wiki/Executable_and_Linkable_Format
    // (There are some good diagrams at the link)
    public void writeHeader(ParseNode node) {
        // The header for the "compiled" output
        out.println("digraph ParseTree {");
        out.printf("\t{\"%s\" [label=\"%s\", shape=diamond]};\n", node, node.getNodeName());
    }

    // Our output requires a footer as well.
    public void writeFooter() {
        out.println("}");
    }

    /**
     * To open a browser window...
     *
     * FEEL FREE TO IGNORE THIS!!! It's just for opening the default browser, if
     * desired.
     */
    static void openWebGraphViz() {
        final var WEBGRAPHVIZ_HOME = "http://www.webgraphviz.com/";

        final var MSG
                = "To visualize the output, Copy/Paste the \n"
                + "parser output into: http://www.webgraphviz.com\n";

        // Open the default browser with the url:
        try {
            final URI webGraphvizURI = new URI(WEBGRAPHVIZ_HOME);
            final Desktop desktop = Desktop.getDesktop();

            if (Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.BROWSE)) {
                out.println(MSG);
                var response = JOptionPane.showConfirmDialog(null, MSG + "\nOpen Web Graphviz Page?", "Open Web Graphviz Page", JOptionPane.YES_NO_OPTION);

                if (response == JOptionPane.YES_OPTION) {
                    desktop.browse(webGraphvizURI);
                }
            }
        } catch (IOException | URISyntaxException ex) {
            java.util.logging.Logger.getAnonymousLogger().log(java.util.logging.Level.WARNING, "Could not open browser", ex);
        }
    }

}
