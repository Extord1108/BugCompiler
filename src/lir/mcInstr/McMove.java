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

    @Override
    public String toString() {
        StringBuilder stb = new StringBuilder();
        //  对出现全局变量的情况特殊处理
        if(useOperands.get(0) instanceof Operand.Global){
            stb.append("movw\t").append(defOperands.get(0) + ",\t").append(":lower16:").append(useOperands.get(0)).append("\n");
            stb.append("\tmovt\t").append(defOperands.get(0) + ",\t").append(":upper16:").append(useOperands.get(0));
            return stb.toString();
        }

        if(isType("Float")) {
            stb.append("vmov.F32\t").append(defOperands.get(0) + ",\t" + useOperands.get(0));
        } else {
            assert isType("Integer");
            stb.append("mov\t" + defOperands.get(0) + ",\t" + useOperands.get(0));
        }
        return stb.toString();
    }
}
