package ir.instruction;

import ir.BasicBlock;
import ir.Value;
import ir.type.VoidType;

public class Jump extends Instr {

    private BasicBlock targetBlock;

    public Jump(BasicBlock targetBlock, BasicBlock basicBlock) {
        super(VoidType.getInstance(), basicBlock);
        this.targetBlock = targetBlock;
        this.addUse(targetBlock);
    }

    public BasicBlock getTargetBlock() {
        return targetBlock;
    }

    @Override
    public String toString() {
        return "br label %" + targetBlock.getName();
    }
}
