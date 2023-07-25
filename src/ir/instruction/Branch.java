package ir.instruction;

import ir.BasicBlock;
import ir.Value;
import ir.type.Type;
import ir.type.VoidType;

public class Branch extends Instr{

    private Value cond;
    private BasicBlock thenBlock;
    private BasicBlock elseBlock;

    public Branch(Value cond, BasicBlock thenBlock, BasicBlock elseBlock, BasicBlock basicBlock) {
        super(VoidType.getInstance(), basicBlock);
        this.cond = cond;
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
    }
}
