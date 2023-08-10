package ir.instruction;

import ir.BasicBlock;
import ir.Value;
import ir.type.VoidType;

public class Jump extends Instr {

    public Jump(BasicBlock targetBlock, BasicBlock basicBlock) {
        super(VoidType.getInstance(), basicBlock);
        this.addUse(targetBlock);
    }

    public Jump(BasicBlock targetBlock) {
        this.type = VoidType.getInstance();
        this.addUse(targetBlock);
    }

    public BasicBlock getTargetBlock() {
        return (BasicBlock) getUse(0);
    }

    @Override
    public String toString() {
        return "br label %" + getTargetBlock().getName();
    }
}
