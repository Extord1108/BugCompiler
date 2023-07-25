package ir.instruction;

import ir.BasicBlock;
import ir.type.Type;
import ir.Value;
import frontend.semantic.OpTree;

public class Binary extends Instr {

    private OpTree.Operator op;
    Value left, right;

    public Binary(Type type, OpTree.Operator op, Value left, Value right, BasicBlock basicBlock) {
        super(type, basicBlock);
        this.op = op;
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        return this.getName() + " = " + op.toString() + " " + this.getType().toString() + " "
                + this.getLeft().toString() + ", " + this.getRight().toString();
    }

    public OpTree.Operator getOp() {
        return op;
    }

    public Value getLeft() {
        return left;
    }

    public Value getRight() {
        return right;
    }
}
