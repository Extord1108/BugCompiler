package lir;

import ir.BasicBlock;
import lir.mcInstr.McInstr;
import util.MyList;
import util.MyNode;

import java.util.HashSet;
import java.util.Set;

public class McBlock extends MyNode {
    private MyList<McInstr> mcInstrs = new MyList<>();
    private BasicBlock basicBlock;
    private McFunction mcFunction;
    private String name;
    private Set<McBlock> predMcBlocks = new HashSet<>();
    private Set<McBlock> succMcBlocks = new HashSet<>();

    public Set<Operand> liveUseSet;
    public Set<Operand> liveDefSet;
    public Set<Operand> liveInSet;
    public Set<Operand> liveOutSet;

    public McBlock(BasicBlock basicBlock){
        this.basicBlock = basicBlock;
        this.name = basicBlock.getName();
    }

    public String getName() {
        return name;
    }

    public MyList<McInstr> getMcInstrs() {
        return mcInstrs;
    }

    public McInstr getMcLastInstr() {
        return mcInstrs.getLast();
    }

    public void addInstr(McInstr mcInstr){
        mcInstrs.insertTail(mcInstr);
    }

    public void addPreMcBlock(McBlock block){
        predMcBlocks.add(block);
    }

    public void addSuccMcBlock(McBlock block) {
        succMcBlocks.add(block);
    }

    public Set<McBlock> getPredMcBlocks() {
        return predMcBlocks;
    }

    public Set<McBlock> getSuccMcBlocks() {
        return succMcBlocks;
    }

    public BasicBlock getBasicBlock() {
        return basicBlock;
    }

    public void setMcFunction(McFunction mcFunction) {
        this.mcFunction = mcFunction;
    }
}
