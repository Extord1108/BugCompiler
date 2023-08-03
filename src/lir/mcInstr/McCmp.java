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
}


