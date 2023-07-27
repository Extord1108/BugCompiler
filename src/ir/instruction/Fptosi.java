package ir.instruction;

import ir.BasicBlock;
import ir.Value;
import ir.type.Int32Type;
import ir.type.Type;

public class Fptosi extends Instr {
    Value value;

    public Fptosi(Value value, BasicBlock basicBlock) {
        super(Int32Type.getInstance(), basicBlock);
        this.value = value;
    }

    @Override
    public String toString() {
        return this.getName() + " = fptosi " + value.getType() + " " + value.getName() + " to " + this.getType();
    }
}
