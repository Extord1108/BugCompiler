package ir.instruction;

import ir.Value;
import frontend.semantic.OpTree;
import ir.BasicBlock;
import ir.type.Int1Type;

public class Fcmp extends Instr {
    Value lhs, rhs;
    OpTree.Operator op;

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