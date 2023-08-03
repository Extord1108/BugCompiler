package lir.mcInstr;

import lir.McBlock;
import lir.Operand;

public class McVcvt extends McInstr{

    public enum VcvtType {
        f2i,
        i2f,
    }

    public VcvtType vcvtType;
    public McVcvt(VcvtType vcvtType, Operand dst, Operand src,McBlock mcBlock) {
        super(mcBlock);
        this.vcvtType = vcvtType;
        defOperands.add(dst);
        useOperands.add(src);
    }
}
