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
    }

    @Override
    public String toString() {
        return this.getName() + " = " + op + " " + this.getType() + " " + this.getVal();
    }

    public Value getVal() {
        return val;
    }

}
