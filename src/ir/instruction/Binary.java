package ir.instruction;

import ir.BasicBlock;
import ir.type.Type;
import ir.Value;
import frontend.semantic.OpTree;

public class Binary extends Instr {

    private OpTree.Operator op;

    public Binary(Type type, OpTree.Operator op, Value left, Value right, BasicBlock basicBlock) {
        super(type, basicBlock);
        this.op = op;
    }

}
