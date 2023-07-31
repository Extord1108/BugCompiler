package ir.instruction;

import ir.BasicBlock;
import ir.Value;
import ir.type.Int32Type;
import ir.type.Type;

public class Zext extends Instr{

    Value value;

    public Zext(Value value, BasicBlock basicBlock) {
        super(Int32Type.getInstance(), basicBlock);
        this.value =value;
        this.addUse(value);
    }

    public String toString() {
        return this.getName() + " = zext " + value.getType() + " "+ value.getName() + " to " + this.type;
    }
}
