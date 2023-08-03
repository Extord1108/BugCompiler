package lir.mcInstr;

import lir.McBlock;
import lir.Operand;

import java.util.ArrayList;

public class McMove extends McInstr{

    public McMove(Operand dstOp, Operand srcOp, McBlock mcBlock) {
        super(mcBlock);
        defOperands.add(dstOp);
        useOperands.add(srcOp);
    }

    public McMove(Cond cond,Operand dstOp, Operand srcOp, McBlock mcBlock) {
        super(mcBlock);
        this.cond = cond;
        defOperands.add(dstOp);
        useOperands.add(srcOp);
    }
}
