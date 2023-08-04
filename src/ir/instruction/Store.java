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
        this.addUse(value);
        this.addUse(address);
    }

    public Value getValue() {
        return value;
    }

    public Value getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return "store " + value.getType().toString() + " " + value.getName() + ", " + address.getType().toString()
                + " " + address.getName();
    }
}
