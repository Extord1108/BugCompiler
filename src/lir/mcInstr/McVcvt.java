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

    @Override
    public String toString() {
        StringBuilder stb = new StringBuilder();
        stb.append("vcvt.");
        if (vcvtType.equals(VcvtType.f2i)) {
            stb.append("S32.F32\t");
        } else {
            assert vcvtType.equals(vcvtType.i2f);
            stb.append("F32.S32\t");
        }
        stb.append(defOperands.get(0)+ ",\t" + useOperands.get(0));
        return stb.toString();
    }
}
