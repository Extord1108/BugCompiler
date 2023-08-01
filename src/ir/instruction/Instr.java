package ir.instruction;

import ir.BasicBlock;
import ir.Used;
import ir.Value;
import ir.type.Type;

import java.util.ArrayList;

public class Instr extends Value {
    private static int count = 0;
    private BasicBlock basicBlock;
    private ArrayList<Value> uses; // 使用了的value

    public Instr(Type type, BasicBlock basicBlock) {
        this.type = type;
        this.basicBlock = basicBlock;
        this.name = "%r" + count++;
        this.uses = new ArrayList<>();
        basicBlock.addInstr(this);
    }

    public Instr(Type type, BasicBlock basicBlock, boolean head) {
        this.type = type;
        this.basicBlock = basicBlock;
        this.name = "%r" + count++;
        this.uses = new ArrayList<>();
//        basicBlock.addInstrHead(this);
        basicBlock.getFunction().getBasicBlocks().get(0).addInstrHead(this);
    }

    public ArrayList<Value> getUses() {
        return uses;
    }

    public Value getUse(int i){
        return uses.get(i);
    }

    public void addUse(Value value){
        uses.add(value);
        value.addUsed(new Used(this, value));
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
