package ir.instruction;

import ir.BasicBlock;
import ir.Value;
import ir.type.Type;

public class Instr extends Value {
    private BasicBlock basicBlock;

    public Instr(Type type, BasicBlock basicBlock){
        this.type = type;
        this.basicBlock = basicBlock;
        basicBlock.addInstr(this);
    }

    public BasicBlock getBasicBlock() {
        return basicBlock;
    }

    public void setBasicBlock(BasicBlock basicBlock) {
        this.basicBlock = basicBlock;
    }
}
