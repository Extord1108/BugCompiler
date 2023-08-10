package lir.mcInstr;

import backend.CodeGen;
import frontend.parser.Visitor;
import lir.McBlock;
import lir.Operand;

import java.util.ArrayList;

public class McMove extends McInstr{

    public McMove(Operand dstOp, Operand srcOp, McBlock mcBlock) {
        super(mcBlock);
        assert dstOp != null;
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
            Operand use = useOperands.get(0);
            if(use instanceof Operand.Imm &&
                    !CodeGen.canImmSaved(((Operand.Imm) use).getIntNumber())){
                int imm = ((Operand.Imm) use).getIntNumber();
                int lowImm = (imm << 16) >>> 16;
                stb.append("movw").append(cond).append("\t").
                        append(defOperands.get(0)).append(",\t#").append(lowImm);
                int highImm = imm >>> 16;
                if (highImm != 0) {
                    stb.append("\n\tmovt").append(cond).append("\t").
                            append(defOperands.get(0)).append(",\t#").append(highImm);
                }
            } else {
                stb.append("mov" + cond  + "\t"+ defOperands.get(0) + ",\t" + useOperands.get(0));
            }
        }
        return stb.toString();
    }
}
