package ir.instruction;

import ir.BasicBlock;
import ir.Value;
import ir.type.PointerType;
import ir.type.Type;

public class Load extends Instr{


    public Load(Value pointer, BasicBlock basicBlock) {
        super(((PointerType)(pointer.getType())).getBasicType(), basicBlock);
        this.addUse(pointer);
    }

    public Value getPointer() {
        return getUse(0);
    }

    @Override
    public String toString() {
        return name + " = load " + type + ", " + getPointer().getType() + " " + getPointer().getName();
    }
}
