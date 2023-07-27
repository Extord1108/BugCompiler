package ir.instruction;

import ir.BasicBlock;
import ir.Value;
import ir.type.FloatType;
import ir.type.Type;

public class Sitofp extends Instr {
    public Value value;

    public Sitofp(Value value, BasicBlock basicBlock) {
        super(FloatType.getInstance(), basicBlock);
        this.value = value;
    }

    @Override
    public String toString() {
        return this.getName() + " = sitofp " + value.getType() + " " + value.getName() + " to " + this.getType();
    }
}
