package ir.instruction;

import ir.BasicBlock;
import ir.Value;
import ir.type.Int1Type;
import ir.type.Type;
import ir.type.VoidType;

public class Branch extends Instr {


    public Branch(Value cond, BasicBlock thenBlock, BasicBlock elseBlock, BasicBlock basicBlock) {
        super(Int1Type.getInstance(), basicBlock);
        this.addUse(cond);
        this.addUse(thenBlock);
        this.addUse(elseBlock);
    }

    public Value getCond() {
        return getUse(0);
    }

    public BasicBlock getThenBlock() {
        return (BasicBlock) getUse(1);
    }

    public BasicBlock getElseBlock() {
        return (BasicBlock)getUse(2);
    }

    public void setThenBlock(BasicBlock basicBlock) {
        replaceUse(getThenBlock(), basicBlock);
    }

    public void setElseBlock(BasicBlock basicBlock) {
        replaceUse(getElseBlock(), basicBlock);
    }

    @Override
    public Instr clone(BasicBlock bb){
        this.cloneInstr = new Branch(this.getCond().getClone(), this.getThenBlock().getClone(), this.getElseBlock().getClone(), bb);
        return this.cloneInstr;
    }

    @Override
    public String toString() {
        return "br i1 " + getCond().getName() + ", label %" + getThenBlock().getName() + ", label %" + getElseBlock().getName();
    }
}
