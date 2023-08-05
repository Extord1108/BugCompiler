package ir.instruction;

import ir.BasicBlock;
import ir.type.FloatType;
import ir.type.Type;
import ir.Value;
import frontend.semantic.OpTree;

public class Unary extends Instr {

    private OpTree.Operator op;

    public Unary(Type type, OpTree.Operator op, Value val, BasicBlock basicBlock) {
        super(type, basicBlock);
        this.op = op;
        this.addUse(val);
    }

    public OpTree.Operator getOp() {
        return op;
    }

    @Override
    public String toString() {

        if(op == OpTree.Operator.Neg)
        {
            if(type instanceof ir.type.FloatType)
            return this.getName() + " = fneg " + this.getType() + " " + getVal().getName();
            else
            return this.getName() + " = sub " + this.getType() + " 0, " + getVal().getName();
        }
        else if (op == OpTree.Operator.Not)
        {
            if(getVal().getType() instanceof FloatType)
                return this.getName() + " = fcmp oeq " + getVal().getType() + " " + getVal().getName() + ", 0x0";
            else
            return this.getName() + " = icmp eq " + getVal().getType() + " " + getVal().getName() + ", 0";
        }
        else
        return this.getName() + " = " + op + " " + this.getType() + " 0, " + getVal().getName();
    }

    public Value getVal() {
        return getUse(0);
    }

}
