package ir.instruction;

import ir.BasicBlock;
import ir.type.FloatType;
import ir.type.Type;
import ir.Value;
import frontend.semantic.OpTree;

public class Unary extends Instr {

    private OpTree.Operator op;
    private Value val;

    public Unary(Type type, OpTree.Operator op, Value val, BasicBlock basicBlock) {
        super(type, basicBlock);
        this.op = op;
        this.val = val;
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
            return this.getName() + " = fneg " + this.getType() + " " + this.val.getName();
            else
            return this.getName() + " = sub " + this.getType() + " 0, " + this.val.getName();
        }
        else if (op == OpTree.Operator.Not)
        {
            if(this.val.getType() instanceof FloatType)
                return this.getName() + " = fcmp oeq " + this.val.getType() + " " + this.val.getName() + ", 0x0";
            else
            return this.getName() + " = icmp eq " + this.val.getType() + " " + this.val.getName() + ", 0";
        }
        else
        return this.getName() + " = " + op + " " + this.getType() + " 0, " + this.val.getName();
    }

    public Value getVal() {
        return val;
    }

}
