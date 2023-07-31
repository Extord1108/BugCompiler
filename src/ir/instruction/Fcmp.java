package ir.instruction;

import ir.Value;
import frontend.semantic.OpTree;
import ir.BasicBlock;
import ir.type.Int1Type;

public class Fcmp extends Instr {
    Value lhs, rhs;
    OpTree.Operator op;

    public Fcmp(Value lhs, Value rhs, OpTree.Operator op, BasicBlock basicBlock) {
        super(Int1Type.getInstance(), basicBlock);
        this.lhs = lhs;
        this.rhs = rhs;
        this.op = op;
        this.addUse(lhs);
        this.addUse(rhs);
    }

    @Override
    public String toString() {
        return this.getName() + " = fcmp " + op.getfName() + " " + lhs.getType() + " " + lhs.getName() + ", "
                + rhs.getName();
    }
}
