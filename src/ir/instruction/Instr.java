package ir.instruction;

import ir.BasicBlock;
import ir.Value;

public class Instr extends Value {
    private BasicBlock basicBlock;

    public BasicBlock getBasicBlock() {
        return basicBlock;
    }

    public void setBasicBlock(BasicBlock basicBlock) {
        this.basicBlock = basicBlock;
    }
}
