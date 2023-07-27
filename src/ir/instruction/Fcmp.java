package ir.instruction;

import ir.Value;
import ir.BasicBlock;

public class Fcmp extends Instr {
    Value lhs, rhs;
    Op op;

    public enum Op {
        OEQ("oeq"),
        ONE("one"),
        OGT("ogt"),
        OGE("oge"),
        OLT("olt"),
        OLE("ole");

        private final String name;

        private Op(final String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

    }

    public Fcmp(Value lhs, Value rhs, Op op, BasicBlock basicBlock) {
        super(lhs.getType(), basicBlock);
        this.lhs = lhs;
        this.rhs = rhs;
        this.op = op;
    }

    @Override
    public String toString() {
        return this.getName() + " = fcmp " + op.getName() + " " + lhs.getType() + " " + lhs.getName() + ", "
                + rhs.getName();
    }
}
