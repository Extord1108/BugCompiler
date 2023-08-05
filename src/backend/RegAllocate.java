package backend;

import lir.McBlock;
import lir.McFunction;
import lir.Operand;
import lir.mcInstr.McInstr;
import util.MyNode;

import java.util.ArrayList;
import java.util.HashSet;

public class RegAllocate {
    public ArrayList<McFunction> mcFunctions;
    private int K = 32; // 着色数
    private String type = "Float"; // 寄存器分配类型
    public McFunction curMcFunc;

    public RegAllocate(ArrayList<McFunction> mcFunctions){
        this.mcFunctions = mcFunctions;
    }

    public void alloc(){
        for(McFunction mcFunction: mcFunctions) {
            if(mcFunction.getSvrList().size() > 0) {
                K = 32;
                type = "Float";
                init(mcFunction);
                allocate(mcFunction);
            }
            K = 14;
            type = "Integer";
            init(mcFunction);
            allocate(mcFunction);
        }
    }

    private void init(McFunction mcFunction) {

    }

    private void allocate(McFunction mcFunction) {
        curMcFunc = mcFunction;
        while(true) {
            livenessAnalysis();
        }
    }

    private void livenessAnalysis() {
        for(McBlock mcBlock: curMcFunc.getMcBlocks()){
            mcBlock.liveUseSet = new HashSet<>();
            mcBlock.liveDefSet = new HashSet<>();
            for(McInstr mcInstr : mcBlock.getMcInstrs()) {
                // 在这个block中先被use
                for(Operand use: mcInstr.useOperands) {
                    if(use.needColor(type) && !mcBlock.liveDefSet.contains(use)){
                        mcBlock.liveUseSet.add(use);
                    } else{
                        assert true;
                    }
                }
                // 在这个block中写被def
                for(Operand def: mcInstr.defOperands) {
                    if(def.needColor(type) && !mcBlock.liveUseSet.contains(def)) {
                        mcBlock.liveDefSet.add(def);
                    } else {
                        assert true;
                    }
                }
            }
            mcBlock.liveInSet.addAll(mcBlock.liveUseSet);
            mcBlock.liveOutSet = new HashSet<>();
        }
        liveInOutAnalysis();
    }

    private void liveInOutAnalysis() {
        boolean changed = true;
        while (changed){
            changed = false;
            for(MyNode iter = curMcFunc.getMcLastBlock(); iter !=
                    curMcFunc.getMcBlocks().head; iter = iter.getPrev()){
                McBlock mcBlock = (McBlock) iter;
            }
        }
    }
}
