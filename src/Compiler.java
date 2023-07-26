import frontend.parser.SysYLexer;
import frontend.parser.SysYParser;
import frontend.parser.Visitor;
import ir.Manager;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import util.Arg;
import util.OutputHandler;

import java.io.FileOutputStream;
import java.io.IOException;

public class Compiler {

    public static void main(String[] args) {
        Arg arg = Arg.parse(args);
        try {
            var input = CharStreams.fromFileName(arg.srcFile);
            SysYLexer lexer = new SysYLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            SysYParser parser = new SysYParser(tokens);
            ParseTree tree = parser.compUnit();
            var visitor = Visitor.Instance;
            visitor.visit(tree);

            // 输出 LLVM
            FileOutputStream fileOut = OutputHandler.getOutputFile("llvm.txt");
            Manager.getManager().outputLLVM(fileOut);
            OutputHandler.closeOutputFile(fileOut);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
