package ir.instruction;

import ir.BasicBlock;
import ir.Value;
import ir.type.Type;

public class BitCast extends Instr{
    public BitCast(Value value, Type type, BasicBlock basicBlock) {
        super(type, basicBlock);
        this.addUse(value);
    }

    @Override
    public Instr clone(BasicBlock bb){
        this.cloneInstr = new BitCast(this.getUse(0).getClone(), this.getType(), bb);
        return this.cloneInstr;
    }

    @Override
    public String toString() {
        return getName() + " = bitcast " + getUse(0).getType() + " " + getUse(0).getName() + " to " + getType();
    }
}

