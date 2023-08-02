package ir.instruction;

import ir.BasicBlock;
import ir.Value;
import ir.type.Type;

public class BitCast extends Instr{
    Value value;
    public BitCast(Value value, Type type, BasicBlock basicBlock) {
        super(type, basicBlock);
        this.value = value;
        this.addUse(value);
    }

    @Override
    public String toString() {
        return getName() + " = bitcast " + value.getType() + " " + value.getName() + " to " + getType();
    }
}

