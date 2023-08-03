package lir;

import ir.BasicBlock;
import lir.mcInstr.McInstr;
import util.MyList;

import java.util.HashSet;
import java.util.Set;

public class McBlock {
    private MyList<McInstr> mcInstrs = new MyList<>();
    private BasicBlock basicBlock;
    private McFunction mcFunction;
    private Set<McBlock> predMcBlocks = new HashSet<>();
    private Set<McBlock> succMcBlocks = new HashSet<>();

    public McBlock(BasicBlock basicBlock){
        this.basicBlock = basicBlock;
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


    public void setMcFunction(McFunction mcFunction) {
        this.mcFunction = mcFunction;
    }
}
