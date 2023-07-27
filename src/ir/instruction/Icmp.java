package ir.instruction;

import ir.Value;
import ir.BasicBlock;

public class Icmp extends Instr {
    Value lhs, rhs;
    Op op;

    public enum Op {
        EQ("eq"),
        NE("ne"),
        SGT("sgt"),
        SGE("sge"),
        SLT("slt"),
        SLE("sle");

        private final String name;

        private Op(final String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

    }

    public Icmp(Value lhs, Value rhs, Op op, BasicBlock basicBlock) {
        super(lhs.getType(), basicBlock);
        this.lhs = lhs;
        this.rhs = rhs;
        this.op = op;
    }

    @Override
    public String toString() {
        return this.getName() + " = icmp " + op.getName() + " " + lhs.getType() + " " + lhs.getName() + ", "
                + rhs.getName();
    }
}
