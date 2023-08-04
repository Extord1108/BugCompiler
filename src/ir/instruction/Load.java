package ir.instruction;

import ir.BasicBlock;
import ir.Value;
import ir.type.PointerType;
import ir.type.Type;

public class Load extends Instr{

    Value pointer;

    public Load(Value pointer, BasicBlock basicBlock) {
        super(((PointerType)(pointer.getType())).getBasicType(), basicBlock);
        this.pointer = pointer;
        this.addUse(pointer);
    }

    public Value getPointer() {
        return pointer;
    }

    @Override
    public String toString() {
        return name + " = load " + type + ", " + pointer.getType() + " " + pointer.getName();
    }
}
