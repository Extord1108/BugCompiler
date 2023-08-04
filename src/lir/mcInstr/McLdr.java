package lir.mcInstr;

import lir.McBlock;
import lir.Operand;

public class McLdr extends McInstr{
    public McLdr(Operand dt, Operand addr, McBlock mcBlock) {
        super(mcBlock);
        defOperands.add(dt);
        useOperands.add(addr);
    }
}
