package lir.mcInstr;

import lir.McBlock;
import lir.Operand;

public class McLdr extends McInstr{
    public McLdr(Operand dt, Operand addr, McBlock mcBlock) {
        super(mcBlock);
        defOperands.add(dt);
        useOperands.add(addr);
    }

    public McLdr(Operand dt, Operand addr, Operand offset, McBlock mcBlock) {
        super(mcBlock);
        defOperands.add(dt);
        useOperands.add(addr);
        useOperands.add(offset);
    }

    public McLdr(Operand dt, Operand addr, Operand offset, McBlock mcBlock, boolean insert) {
        super(mcBlock, insert);
        defOperands.add(dt);
        useOperands.add(addr);
        useOperands.add(offset);
    }

    public boolean useVFP() {
        return defOperands.get(0).isFloat();
    }
    @Override
    public String toString() {
        StringBuilder stb = new StringBuilder();
        if(!useVFP()) {
            stb.append("ldr" + "\t" + defOperands.get(0) + ",\t" + "[" + useOperands.get(0));
            if(useOperands.size() == 2)
                stb.append(",\t" + useOperands.get(1));
            stb.append("]");
        } else {
            stb.append("vldr.F32" + "\t" + defOperands.get(0) + ",\t" + "[" + useOperands.get(0));
            if(useOperands.size() == 2)
                stb.append(",\t" + useOperands.get(1));
            stb.append("]");
        }
        return stb.toString();
    }
}
