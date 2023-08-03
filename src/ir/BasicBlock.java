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

    private ArrayList<BasicBlock> precBBlocks = new ArrayList<>();
    private ArrayList<BasicBlock> succBBlocks = new ArrayList<>();
    private int domTreeDepth = 0;



    private int label;
    private static Integer block_count = 0;

    public BasicBlock() {
        this.label = ++block_count;
        this.name = "b" + this.label;
    }

    public BasicBlock(Function function) {
        this.label = ++block_count;
        this.name = "b" + this.label;
        this.function = function;
        function.addAtEnd(this);
    }

    public void addFunction(Function function) {
        this.function = function;
        function.addAtEnd(this);
    }

    public MyList<Instr> getInstrs() {
        return instrs;
    }

    public void addInstrHead(Instr instr) {
        instrs.insertHead(instr);
    }

    public void addInstr(Instr instr) {
        instrs.insertTail(instr);
    }

    public boolean isTerminated() {
        if(instrs.size() == 0)
            return false;
        return getLast() instanceof Return;
    }

    public Instr getLast() {
        return instrs.getLast();
    }

    public Function getFunction() {
        return function;
    }

    public void setSuccessors(ArrayList<BasicBlock> succBBlocks) {
        this.succBBlocks = succBBlocks;
    }
    public void setPredecessors(ArrayList<BasicBlock> precBBlocks) {
        this.precBBlocks = precBBlocks;
    }

    public ArrayList<BasicBlock> getSuccessors() {
        return succBBlocks;
    }

    public ArrayList<BasicBlock> getPredecessors() {
        return precBBlocks;
    }

    public int getDomTreeDepth() {
        return domTreeDepth;
    }
    public void setDomTreeDepth(int domTreeDepth) {
        this.domTreeDepth = domTreeDepth;
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
