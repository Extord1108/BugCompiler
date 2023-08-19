package ir.instruction;

import ir.BasicBlock;
import ir.Value;
import ir.type.Int32Type;
import ir.type.Type;

public class Fptosi extends Instr {

    public Fptosi(Value value, BasicBlock basicBlock) {
        super(Int32Type.getInstance(), basicBlock);
        this.addUse(value);
    }

    @Override
    public Instr clone(BasicBlock bb){
        this.cloneInstr = new Fptosi(this.getUse(0).getClone(), bb);
        return this.cloneInstr;
    }

    @Override
    public String toString() {
        return this.getName() + " = fptosi " + getUse(0).getType() + " " + getUse(0).getName() + " to " + this.getType();
    }

    public Value getValue() {
        return this.getUse(0);
    }
}
