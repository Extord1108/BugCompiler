package ir.instruction;

import ir.BasicBlock;
import ir.Used;
import ir.Value;
import ir.type.Type;

import java.util.ArrayList;

public class Pcopy extends Instr{

    private ArrayList<Value> from = new ArrayList<>();
    private ArrayList<Value> to = new ArrayList<>();
    public Pcopy(){
    }

    public void addFromAndTo(Value from, Value to){
        this.from.add(from);
        this.to.add(to);
    }

    public Value getFrom(int i){
        return from.get(i);
    }

    public void setFrom(ArrayList<Value> from) {
        this.from = from;
    }

    public void setTo(ArrayList<Value> to) {
        this.to = to;
    }

    public Value getTo(int i){
        return to.get(i);
    }

    public int getSize(){
        return from.size();
    }

    public boolean fromContains(Value value){
        return from.contains(value);
    }

    public int getFromIndex(Value value){
        return from.indexOf(value);
    }

    public void modifyFrom(int index, Value newVal){
        from.set(index, newVal);
    }

    public void removeFromAndTo(int index){
        from.remove(index);
        to.remove(index);
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append("pcopy ");
        for(int i = 0; i < from.size(); i++){
            ret.append(from.get(i).toString());
            ret.append(" ");
            ret.append(to.get(i).toString());
            if(i != from.size() - 1){
                ret.append(", ");
            }
        }
        return ret.toString();
    }


    public void setBasicBlock(BasicBlock basicBlock) {
        if(this.basicBlock!=null)
            this.basicBlock.getInstrs().remove(this);
        this.basicBlock = basicBlock;
        if(basicBlock.isTerminated()){
            Instr lastInstr = basicBlock.getInstrs().getLast();
            basicBlock.getInstrs().insertBefore(lastInstr, this);
        }else{
            basicBlock.addInstr(this);
        }
    }
}
