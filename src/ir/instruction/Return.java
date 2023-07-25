package ir.instruction;

import ir.BasicBlock;
import ir.type.Type;

public class Return extends Instr{
    public Return(Type type, BasicBlock basicBlock) {
        super(type, basicBlock);
    }
}
