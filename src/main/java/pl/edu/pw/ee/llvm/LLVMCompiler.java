package pl.edu.pw.ee.llvm;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import pl.edu.pw.ee.HolyJavaLexer;
import pl.edu.pw.ee.HolyJavaParser;

import java.io.IOException;

public class LLVMCompiler {

    public void compile(String filename) throws IOException {
        final var input = CharStreams.fromFileName(filename);
        final var lexer = new HolyJavaLexer(input);
        final var tokens = new CommonTokenStream(lexer);
        final var parser = new HolyJavaParser(tokens);
        final var tree = parser.programme();
        final var walker = new ParseTreeWalker();
        final var actions = new LLVMActions();
        walker.walk(actions, tree);
    }

}
