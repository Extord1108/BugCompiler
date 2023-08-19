package ir.instruction;

import ir.BasicBlock;
import ir.Value;
import ir.type.ArrayType;
import ir.type.PointerType;
import ir.type.Type;

import java.util.ArrayList;

public class GetElementPtr extends Instr{


    public GetElementPtr(Type type, Value pointer, ArrayList<Value> idxList, BasicBlock basicBlock) {
        super(new PointerType(type.getContentType()), basicBlock);
        this.addUse(pointer);
        for(Value idx: idxList){
            this.addUse(idx);
        }
    }

    public Value getPointer() {
        return getUse(0);
    }

    public ArrayList<Value> getIdxList() {
        ArrayList<Value> ret = new ArrayList<>();
        for(int i = 1; i < getUses().size(); i++){
            ret.add(getUse(i));
        }
        return ret;
    }

    @Override
    public Instr clone(BasicBlock bb){
        ArrayList<Value> idxList = new ArrayList<>();
        for(Value idx: this.getIdxList()){
            idxList.add(idx.getClone());
        }
        this.cloneInstr = new GetElementPtr(((PointerType)getType()).getContentType(), getPointer().getClone(), idxList, bb);
        return this.cloneInstr;
    }

    @Override
    public String toString() {
        String ret = name + " = getelementptr inbounds " + getPointer().getType().getBasicType() + ", " + getPointer().getType() + " "
                + getPointer().getName() + ", ";
        for(int i = 0; i < getIdxList().size(); i++){
            ret += getIdxList().get(i).getType() + " " + getIdxList().get(i).getName();
            if(i != getIdxList().size() - 1)
                ret += ", ";
        }
        return  ret;
    }
}
