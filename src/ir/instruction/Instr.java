package ir.instruction;

import ir.BasicBlock;
import ir.Value;
import ir.type.Type;

public class Instr extends Value {
    private static int count = 0;
    private BasicBlock basicBlock;

    public Instr(Type type, BasicBlock basicBlock) {
        this.type = type;
        this.basicBlock = basicBlock;
        this.name = "%" + count++;
        basicBlock.addInstr(this);
    }

    public Instr(Type type, BasicBlock basicBlock, boolean head) {
        this.type = type;
        this.basicBlock = basicBlock;
        this.name = "%" + count++;
        basicBlock.addInstrHead(this);
    }



    public static int getCount() {

        return count++;
    }

    public BasicBlock getBasicBlock() {
        return basicBlock;
    }

    public void setBasicBlock(BasicBlock basicBlock) {
        this.basicBlock = basicBlock;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
