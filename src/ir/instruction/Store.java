package ir.instruction;

import ir.BasicBlock;
import ir.Value;
import ir.type.Type;
import ir.type.VoidType;

public class Store extends Instr {

    public Store(Value value, Value address, BasicBlock basicBlock) {
        super(VoidType.getInstance(), basicBlock);
        this.addUse(value);
        this.addUse(address);
    }

    public Store(Value value, Value address, BasicBlock basicBlock, boolean insert) {
        this.type = VoidType.getInstance();
        this.basicBlock = basicBlock;
        this.addUse(value);
        this.addUse(address);
    }

    public Value getValue() {
        return getUse(0);
    }

    public Value getAddress() {
        return getUse(1);
    }

    @Override
    public Instr clone(BasicBlock bb){
        this.cloneInstr = new Store(getValue().getClone(), getAddress().getClone(), bb);
        return this.cloneInstr;
    }

    @Override
    public String toString() {
        return "store " + getValue().getType().toString() + " " + getValue().getName() + ", " + getAddress().getType().toString()
                + " " + getAddress().getName();
    }
}
