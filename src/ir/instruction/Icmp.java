package ir.instruction;

import ir.Value;
import ir.type.Int1Type;
import frontend.semantic.OpTree;
import ir.BasicBlock;

public class Icmp extends Instr {
    Value lhs, rhs;
    OpTree.Operator op;

    public Icmp(Value lhs, Value rhs, OpTree.Operator op, BasicBlock basicBlock) {
        super(Int1Type.getInstance(), basicBlock);
        this.lhs = lhs;
        this.rhs = rhs;
        this.op = op;
        this.addUse(lhs);
        this.addUse(rhs);
    }

    public Value getLhs() {
        return lhs;
    }

    public Value getRhs() {
        return rhs;
    }

    public OpTree.Operator getOp() {
        return op;
    }

    @Override
    public String toString() {
        return this.getName() + " = icmp " + op + " " + lhs.getType() + " " + lhs.getName() + ", "
                + rhs.getName();
    }
}
