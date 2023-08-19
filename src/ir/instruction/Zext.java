package ir.instruction;

import ir.BasicBlock;
import ir.Value;
import ir.type.Int32Type;
import ir.type.Type;

public class Zext extends Instr{

    public Zext(Value value, BasicBlock basicBlock) {
        super(Int32Type.getInstance(), basicBlock);
        this.addUse(value);
    }

    public Value getValue() {
        return this.getUse(0);
    }

    @Override
    public Instr clone(BasicBlock bb){
        this.cloneInstr = new Zext(this.getValue().getClone(), bb);
        return this.cloneInstr;
    }

    public String toString() {
        return this.getName() + " = zext " + getValue().getType() + " "+ getValue().getName() + " to " + this.type;
    }
}
