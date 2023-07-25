package ir.instruction;

import ir.BasicBlock;
import ir.type.Type;

// 内存分配
public class Alloc extends Instr {

    public Alloc(Type type, BasicBlock basicBlock) {
        super(type, basicBlock);
    }

    @Override
    public String toString() {
        return this.getName() + " = alloca " + this.getType().toString() + "\n";
    }
}
