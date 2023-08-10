import backend.CodeGen;
import backend.RegAllocate;
import frontend.parser.SysYLexer;
import frontend.parser.SysYParser;
import frontend.parser.Visitor;
import ir.BasicBlock;
import manager.Manager;

import midend.MidEndRunner;
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

//             if (!arg.llvmFile.isEmpty()) {
//                 FileOutputStream llvmOut = OutputHandler.getOutputFile(arg.llvmFile.split("\\.")[0]+"-pure." + arg.llvmFile.split("\\.")[1]);
//                 Manager.getManager().outputLLVM(llvmOut);
//                 OutputHandler.closeOutputFile(llvmOut);
//             }

            var midEndRunner = new MidEndRunner(Manager.getFunctions(), Manager.getGlobals(), arg.opt);
            midEndRunner.run();

            // 输出 LLVM
            if (!arg.llvmFile.isEmpty()) {
                FileOutputStream llvmOut = OutputHandler.getOutputFile(arg.llvmFile);
                Manager.getManager().outputLLVM(llvmOut);
                OutputHandler.closeOutputFile(llvmOut);
            }
          System.out.println("CodeGen begin");
          var codeGen = CodeGen.Instance;
          codeGen.gen();
          System.out.println("CodeGen end");

          System.out.println("Alloc begin");
          var regAllocate = new RegAllocate(Manager.getMcFunclist());
          regAllocate.alloc();
          System.out.println("Alloc end");

            // 输出 机器代码arm
            if (!arg.targetFile.isEmpty()) {
                FileOutputStream armOut = OutputHandler.getOutputFile(arg.targetFile);
                Manager.getManager().outputArm(armOut);
                OutputHandler.closeOutputFile(armOut);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
