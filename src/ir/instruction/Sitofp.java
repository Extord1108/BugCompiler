package ir.instruction;

import ir.BasicBlock;
import ir.Value;
import ir.type.FloatType;
import ir.type.Type;

public class Sitofp extends Instr {

    public Sitofp(Value value, BasicBlock basicBlock) {
        super(FloatType.getInstance(), basicBlock);
        this.addUse(value);

    }

    @Override
    public Instr clone(BasicBlock bb){
        this.cloneInstr = new Sitofp(this.getUse(0).getClone(), bb);
        return this.cloneInstr;
    }

    @Override
    public String toString() {
        return this.getName() + " = sitofp " + getUse(0).getType() + " " + getUse(0).getName() + " to " + this.getType();
    }

    public Value getValue() {
        return this.getUse(0);
    }
}
