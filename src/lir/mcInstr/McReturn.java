package lir.mcInstr;

import backend.CodeGen;
import lir.McBlock;
import lir.Operand;

import java.util.ArrayList;

public class McReturn extends McInstr{


    public McReturn(Operand ret, McBlock mcBlock) {
        super(mcBlock);
        useOperands.add(ret);
        defOperands.add(Operand.PhyReg.getPhyReg("r0"));
    }

    public McReturn(McBlock mcBlock){
        super(mcBlock);
    }

    @Override
    public String toString() {
        StringBuilder stb = new StringBuilder();
        StringBuilder popRegs = new StringBuilder();
        StringBuilder popFRegs = new StringBuilder();
//        for(Operand reg: mcBlock.getMcFunction().usedPhyRegs) {
//
//        }
        stb.append("vpop\t{}\n");
        stb.append("\tpop\t{}\n");
        stb.append("\tbx\tlr\n");
        return stb.toString();
    }
}
