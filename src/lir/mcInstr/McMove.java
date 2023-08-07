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

    public McMove(Operand dstOp, Operand srcOp, McBlock mcBlock, boolean insert) {
        super(mcBlock, insert);
        defOperands.add(dstOp);
        useOperands.add(srcOp);
    }

    public McMove(Cond cond,Operand dstOp, Operand srcOp, McBlock mcBlock) {
        super(mcBlock);
        this.cond = cond;
        defOperands.add(dstOp);
        useOperands.add(srcOp);
    }

    public Operand getDstOp() {
        return defOperands.get(0);
    }

    public Operand getSrcOp() {
        return useOperands.get(0);
    }

    @Override
    public boolean isType(String type) {
        if(type == "Integer") {
            return (!getDstOp().isFloat()) && (!getSrcOp().isFloat());
        }else {
            assert type == "Float";
            return getDstOp().isFloat() || getSrcOp().isFloat();
        }
    }
}
