package ir.instruction;

import java.util.ArrayList;

import ir.BasicBlock;
import ir.Value;
import ir.type.Type;

public class Phi extends Instr {

    public Phi(Type type, BasicBlock basicBlock,ArrayList<Value> optionValues) {
        super(type, basicBlock);
        basicBlock.getInstrs().remove(this);
        basicBlock.addInstrHead(this);
        //TODO Auto-generated constructor stub
        for(Value value:optionValues) {
            this.addUse(value);
        }
    }

    public void modifyUse(int index,Value value) {
        this.setUse(index, value);
    }

    @Override
    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(this.getName()+" = phi "+this.getType().toString()+" ");
        for(int i=0;i<this.getUses().size();i++) {
            stringBuffer.append("[ "+this.getUse(i).getName()+" , %"+this.getBasicBlock().getPredecessors().get(i).getName()+" ]");
            if(i!=this.getUses().size()-1) {
                stringBuffer.append(", ");
            }
        }
        return stringBuffer.toString();
    }
    
}
