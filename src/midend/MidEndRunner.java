package midend;

import ir.BasicBlock;
import ir.Function;
import ir.GlobalValue;
import ir.instruction.Instr;

import java.util.ArrayList;
import java.util.HashMap;

public class MidEndRunner {
    private HashMap<String, Function> functions;
    private ArrayList<GlobalValue> globals;
    private boolean opt;

    public MidEndRunner(HashMap<String, Function> functions, ArrayList<GlobalValue> globals, boolean opt){
        this.functions = functions;
        this.globals = globals;
        this.opt = opt;
    }

    public void run(){
        new DomainAnalysis(functions, globals).run();
        new Mem2Reg(functions, globals).run();
        if(opt){
            new FunctionInlining(functions, globals).run();
            new DomainAnalysis(functions, globals).run();
            new DeadCodeElimination(functions, globals).run();
            new InstrComb(functions, globals).run();
            new DeadCodeElimination(functions, globals).run();
            new FunctionAnalysis(functions, globals).run();
            new GVN(functions, globals).run();
            new GCM(functions, globals).run();
            new DomainAnalysis(functions, globals).run();
            new ConstantPropagation(functions, globals).run();
            new DomainAnalysis(functions, globals).run();
            new DeadCodeElimination(functions, globals).run();
        }
        new PhiResolution(functions, globals).run();
        return;
    }
}
