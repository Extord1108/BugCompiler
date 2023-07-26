package ir.instruction;

import ir.BasicBlock;
import ir.Value;
import ir.type.Type;
import ir.type.VoidType;

public class Store extends Instr {

    Value value;
    Value address;

    public Store(Value value, Value address, BasicBlock basicBlock) {
        super(VoidType.getInstance(), basicBlock);
        this.value = value;
        this.address = address;
    }

    @Override
    public String toString() {
        return "store " + value.getType().toString() + " " + value.toString() + ", " + address.getType().toString()
                + " " + address.toString() + "\n";
    }
}
