package ir;

import ir.instruction.Call;
import ir.instruction.Instr;
import ir.instruction.Jump;
import ir.instruction.Return;
import util.MyList;

import java.util.ArrayList;

public class BasicBlock extends Value {
    private Function function;

    MyList<Instr> instrs = new MyList<>();

    private int label;
    private static Integer block_count = 0;

    public BasicBlock() {
        this.label = ++block_count;
        this.name = "label" + this.label;
    }

    public BasicBlock(Function function) {
        this.label = ++block_count;
        this.name = "label" + this.label;
        this.function = function;
        function.addAtEnd(this);
    }

    public void addFunction(Function function) {
        this.function = function;
        function.addAtEnd(this);
    }

    public void addInstrHead(Instr instr) {
        instrs.insertHead(instr);
    }

    public void addInstr(Instr instr) {
        instrs.insertTail(instr);
    }

    public boolean isTerminated() {
        return getLast() instanceof Return;
    }

    public Instr getLast() {
        return instrs.getLast();
    }

    public Function getFunction() {
        return function;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(":\n");
        for (Instr instr : instrs) {
            sb.append("\t" + instr.toString() + "\n");
        }
        return sb.toString();
    }
}
