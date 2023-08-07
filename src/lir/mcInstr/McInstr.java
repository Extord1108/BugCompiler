package lir.mcInstr;

import lir.McBlock;
import lir.Operand;
import util.MyNode;

import java.util.ArrayList;

public class McInstr extends MyNode {
    public ArrayList<Operand> defOperands = new ArrayList<>();
    public ArrayList<Operand> useOperands = new ArrayList<>();
    public Cond cond = Cond.Any;

    public McBlock mcBlock;

    public McInstr(McBlock mcBlock){
        this.mcBlock = mcBlock;
        mcBlock.addInstr(this);
    }

    public McInstr(McBlock mcBlock, boolean inSert){
        this.mcBlock = mcBlock;
    }

    public boolean isType(String type) {
        return false;
    }
}
