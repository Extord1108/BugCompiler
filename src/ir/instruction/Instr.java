package ir.instruction;

import ir.BasicBlock;
import ir.Used;
import ir.Value;
import ir.type.Type;

import java.util.ArrayList;

public class Instr extends Value {
    private static int count = 0;
    protected BasicBlock basicBlock;
    private ArrayList<Value> uses; // 使用了的value

    private BasicBlock earliestBasicBlock;
    private BasicBlock latestBasicBlock;

    public Instr(Type type, BasicBlock basicBlock) {
        this.type = type;
        this.basicBlock = basicBlock;
        this.name = "%r" + count++;
        this.uses = new ArrayList<>();
        if(!basicBlock.isTerminated())
        basicBlock.addInstr(this);
    }

    public Instr(Type type, BasicBlock basicBlock, boolean head) {
        this.type = type;
        this.basicBlock = basicBlock.getFunction().getBasicBlocks().get(0);
        this.name = "%r" + count++;
        this.uses = new ArrayList<>();
//        basicBlock.addInstrHead(this);
        basicBlock.getFunction().getBasicBlocks().get(0).addInstrHead(this);
    }

    public Instr(){
        this.uses = new ArrayList<>();
    }

    public ArrayList<Value> getUses() {
        return uses;
    }

    public Value getUse(int i){
        if(i >= uses.size())
            return null;
        return uses.get(i);
    }

    public void addUse(Value value){
        uses.add(value);
        value.addUsed(new Used(this, value));
    }

    public void setUse(int index,Value value){
        uses.set(index, value);
        value.addUsed(new Used(this, value));
    }

    public void replaceUse(Value oldVal, Value newVal) {
        for (int i = 0; i < uses.size(); i++) {
            if (uses.get(i).equals(oldVal)) {
                uses.set(i, newVal);
                newVal.addUsed(new Used(this, newVal));
            }
        }
    }
    /**
     * 对于所有用到自己的地方，用value替换自己
     */
    public void repalceUseofMeto(Value value){
        for(Used used:usedInfo){
            used.getUser().replaceUse(this,value);
        }
    }

    public void removeUse(){
        for(Value use:uses){
            use.removeUser(this);
        }
        uses.clear();
        usedInfo.clear();
    }

    public void remove(){
        this.basicBlock.getInstrs().remove(this);
        removeUse();
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

    public BasicBlock getEarliestBasicBlock() {
        return earliestBasicBlock;
    }

    public void setEarliestBasicBlock(BasicBlock earliestBasicBlock) {
        this.earliestBasicBlock = earliestBasicBlock;
    }

    public BasicBlock getLatestBasicBlock() {
        return latestBasicBlock;
    }

    public void setLatestBasicBlock(BasicBlock latestBasicBlock) {
        this.latestBasicBlock = latestBasicBlock;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
