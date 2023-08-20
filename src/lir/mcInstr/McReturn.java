package lir.mcInstr;

import backend.CodeGen;
import lir.McBlock;
import lir.Operand;

import java.util.ArrayList;

public class McReturn extends McInstr{


    public McReturn(Operand ret, McBlock mcBlock) {
        super(mcBlock);
//        useOperands.add(ret);
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
        int idx = 0;
        for(Operand reg: mcBlock.getMcFunction().usedPhyRegs) {
            idx ++;
            if(((Operand.PhyReg)reg).getIdx() > 3 && !reg.equals(Operand.PhyReg.getPhyReg("sp"))){
                popRegs.append(reg);
                if(idx < mcBlock.getMcFunction().usedPhyRegs.size()) {
                    popRegs.append(", ");
                }
            }

        }
        idx = 0;
        for(Operand reg: mcBlock.getMcFunction().usedFPhyRegs) {
            idx ++;
            if(((Operand.FPhyReg)reg).getIdx() > 15) {
                popFRegs.append(reg);
                if(idx < mcBlock.getMcFunction().usedFPhyRegs.size()) {
                    popFRegs.append(", ");
                }
            }
        }
        boolean needT = false;
        if(!popRegs.toString().equals("")) {
            stb.append("pop\t{"+ popRegs.toString()+ "}\n");
            needT = true;
        }
        if(!popFRegs.toString().equals("")) {
            if(needT) {
                stb.append("\t");
            }
            stb.append("vpop\t{"+ popFRegs.toString()+"}\n");

        }
        if(needT) {
            stb.append("\t");
        }
        stb.append("bx\tlr\n");
        return stb.toString();
    }
}
