package lir;

import ir.BasicBlock;
import lir.mcInstr.McInstr;
import util.MyList;

public class McBlock {
    private MyList<McInstr> mcInstrs = new MyList<>();
    private BasicBlock basicBlock;
    private McFunction mcFunction;

    public McBlock(BasicBlock basicBlock){
        this.basicBlock = basicBlock;
    }

    public void addInstr(McInstr mcInstr){
        mcInstrs.insertTail(mcInstr);
    }

    public void setMcFunction(McFunction mcFunction) {
        this.mcFunction = mcFunction;
    }
}
