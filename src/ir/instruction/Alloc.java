package ir.instruction;

import ir.BasicBlock;
import ir.type.ArrayType;
import ir.type.PointerType;
import ir.type.Type;

// 内存分配
public class Alloc extends Instr {

    public Alloc(Type type, BasicBlock basicBlock) {
        super(new PointerType(type), basicBlock, true);
    }

    public boolean isArrayAlloc() {
        return this.type.getBasicType() instanceof ArrayType;
    }

    @Override
    public Instr clone(BasicBlock bb){
        this.cloneInstr = new Alloc(this.type.getBasicType(), bb);
        return this.cloneInstr;
    }

    @Override
    public String toString() {
        return this.getName() + " = alloca " + this.type.getBasicType().toString();
    }
}
