package ir.instruction;

import ir.BasicBlock;
import ir.type.FloatType;
import ir.type.Type;
import ir.Value;
import frontend.semantic.OpTree;

public class Binary extends Instr {

    private OpTree.Operator op;

    public Binary(Type type, OpTree.Operator op, Value left, Value right, BasicBlock basicBlock) {
        super(type, basicBlock);
        this.op = op;
        this.addUse(left);
        this.addUse(right);
    }

    @Override
    public String toString() {
        if(type instanceof FloatType)
        return this.getName() + " = " + op.getfName() + " " + this.getType() + " " + this.getLeft().getName() + ", " + this.getRight().getName();
        else{
            return this.getName() + " = " + op + " " + this.getType() + " " + this.getLeft().getName() + ", " + this.getRight().getName();
        }
    }

    public OpTree.Operator getOp() {
        return op;
    }

    public Value getLeft() {
        return this.getUse(0);
    }

    public Value getRight() {
        return this.getUse(1);
    }
}
