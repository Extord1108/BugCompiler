package lir.mcInstr;

import lir.McBlock;
import lir.Operand;

public class McCmp extends McInstr {

    public McCmp(Cond cond, Operand lOpd, Operand rOpd, McBlock mcBlock) {
        super(mcBlock);
        this.cond = cond;
        useOperands.add(lOpd);
        useOperands.add(rOpd);
    }

    public boolean useVFP() {
        return useOperands.get(0).isFloat() || useOperands.get(1).isFloat();
    }
    @Override
    public String toString() {
        if(useVFP())
            return "vcmp.F32\t" + useOperands.get(0) + ",\t" + useOperands.get(1);
        return "cmp\t" + useOperands.get(0) + ",\t" + useOperands.get(1);
    }
}


