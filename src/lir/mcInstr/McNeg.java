package lir.mcInstr;

import lir.McBlock;
import lir.Operand;

public class McNeg extends McInstr{
    public McNeg(Operand dst, Operand src, McBlock mcBlock) {
        super(mcBlock);
        defOperands.add(dst);
        useOperands.add(src);
    }

    @Override
    public String toString() {
        return "vneg.F32\t" + defOperands.get(0) + ",\t" + useOperands.get(0);
    }
}
