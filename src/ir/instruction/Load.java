package ir.instruction;

import ir.BasicBlock;
import ir.Value;
import ir.type.PointerType;
import ir.type.Type;

public class Load extends Instr{

    private Value alloc;
    private Instr store;

    public Load(Value pointer, BasicBlock basicBlock) {
        super(((PointerType)(pointer.getType())).getBasicType(), basicBlock);
        this.addUse(pointer);
    }

    public Value getPointer() {
        return getUse(0);
    }

    public void cleanAS()
    {
        alloc = null;
        store = null;
    }

    public void setAlloc(Value alloc){
        this.alloc = alloc;
    }

    public Value getAlloc(){
        return  this.alloc;
    }

    @Override
    public Instr clone(BasicBlock bb){
        this.cloneInstr = new Load(getPointer().getClone(), bb);
        return this.cloneInstr;
    }



    @Override
    public String toString() {
        return name + " = load " + type + ", " + getPointer().getType() + " " + getPointer().getName();
    }
}
