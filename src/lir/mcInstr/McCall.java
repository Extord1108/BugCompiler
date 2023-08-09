package lir.mcInstr;

import lir.McBlock;
import lir.McFunction;
import lir.Operand;

public class McCall extends McInstr{
    public McFunction mcFunction;
    private int sRegIdx;
    private int rRegIdx;
    public McCall(McFunction mcFunction, McBlock mcBlock) {
        super(mcBlock);
        this.mcFunction = mcFunction;
    }

    public void setsRegIdx(int sRegIdx) {
        this.sRegIdx = sRegIdx;
    }

    public void setrRegIdx(int rRegIdx) {
        this.rRegIdx = rRegIdx;
    }

    public void genDefUse() {
        defOperands.add(Operand.PhyReg.getPhyReg("lr"));
        defOperands.add(Operand.PhyReg.getPhyReg("r12"));
        for(int i = 0; i < 4; i++) {
            defOperands.add(Operand.PhyReg.getPhyReg(i));
        }
        for(int i = 0; i < 16; i++) {
            defOperands.add(Operand.FPhyReg.getFPhyReg(i));
        }
        for(int i = 0; i < rRegIdx; i++){
            useOperands.add(Operand.PhyReg.getPhyReg(i));
        }
        for(int i = 0; i < sRegIdx; i++) {
            useOperands.add(Operand.FPhyReg.getFPhyReg(i));
        }
    }

    @Override
    public String toString() {
        return "bl\t" + mcFunction.getName();
    }
}
