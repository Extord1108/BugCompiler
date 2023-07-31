package ir.instruction;

import ir.BasicBlock;
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

    @Override
    public String toString() {

        if(op == OpTree.Operator.Neg)
        {
            if(type instanceof ir.type.FloatType)
            return this.getName() + " = " + OpTree.Operator.Sub.getfName() + " " + this.getType() + " 0, " + this.getVal();
            else
            return this.getName() + " = fneg " + this.getType() + " " + this.getVal();
        }
        else if (op == OpTree.Operator.Not)
        {
            return this.getName() + " = xor " + this.getType() + " 1, " + this.getVal();
        }
        else
        return this.getName() + " = " + op + " " + this.getType() + " 0, " + this.getVal();
    }

    public Value getVal() {
        return val;
    }

}
