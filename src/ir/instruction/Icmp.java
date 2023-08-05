package ir.instruction;

import ir.Value;
import ir.type.Int1Type;
import frontend.semantic.OpTree;
import ir.BasicBlock;

public class Icmp extends Instr {
    OpTree.Operator op;

    public Icmp(Value lhs, Value rhs, OpTree.Operator op, BasicBlock basicBlock) {
        super(Int1Type.getInstance(), basicBlock);
        this.op = op;
        this.addUse(lhs);
        this.addUse(rhs);
    }

    public Value getLhs() {
        return this.getUse(0);
    }

    public Value getRhs() {
        return this.getUse(1);
    }

    public OpTree.Operator getOp() {
        return op;
    }

    @Override
    public String toString() {
        return this.getName() + " = icmp " + op + " " + getLhs().getType() + " " + getLhs().getName() + ", "
                + getRhs().getName();
    }
}
