package lir.mcInstr;

import lir.McBlock;
import lir.Operand;

public class MCShift extends McInstr{

    public enum ShiftType {
        lsl, //逻辑左移
        lsr, //逻辑右移
        asr, //算数右移
        ror, //循环右移
    }

    private  ShiftType type;

    public MCShift(Operand dstOp, Operand srcOp1, Operand srcOp2, ShiftType shiftType, McBlock mcBlock) {
        super(mcBlock);
        this.type = shiftType;
        defOperands.add(dstOp);
        useOperands.add(srcOp1);
        useOperands.add(srcOp2);
    }

    Operand imm;
    public MCShift(ShiftType shiftType, Operand imm) {
        super(null);
        this.type = shiftType;
        this.imm = imm;
    }
}
