package lir.mcInstr;

import lir.McBlock;
import lir.Operand;

public class McNeg extends McInstr{
    public McNeg(Operand dst, Operand src, McBlock mcBlock) {
        super(mcBlock);
        defOperands.add(dst);
        useOperands.add(src);
    }
}
