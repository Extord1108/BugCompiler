package ir.instruction;

import ir.Value;
import frontend.semantic.OpTree;
import ir.BasicBlock;
import ir.type.Int1Type;

public class Fcmp extends Instr {
    OpTree.Operator op;

    public Fcmp(Value lhs, Value rhs, OpTree.Operator op, BasicBlock basicBlock) {
        super(Int1Type.getInstance(), basicBlock);
        this.op = op;
        this.addUse(lhs);
        this.addUse(rhs);
    }

    public Value getLhs() {
        return getUse(0);
    }

    public Value getRhs() {
        return getUse(1);
    }

    public OpTree.Operator getOp() {
        return op;
    }

    @Override
    public String toString() {
        return this.getName() + " = fcmp " + op.getfName() + " " + getLhs().getType() + " " + getLhs().getName() + ", "
                + getRhs().getName();
    }
}
