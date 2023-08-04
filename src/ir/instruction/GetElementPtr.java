package ir.instruction;

import ir.BasicBlock;
import ir.Value;
import ir.type.ArrayType;
import ir.type.PointerType;
import ir.type.Type;

import java.util.ArrayList;

public class GetElementPtr extends Instr{

    Value pointer;
    ArrayList<Value> idxList;

    public GetElementPtr(Type type, Value pointer, ArrayList<Value> idxList, BasicBlock basicBlock) {
        super(new PointerType(type.getContentType()), basicBlock);
        this.pointer = pointer;
        this.idxList = idxList;
        this.addUse(pointer);
        for(Value idx: idxList){
            this.addUse(idx);
        }
    }

    public Value getPointer() {
        return pointer;
    }

    public ArrayList<Value> getIdxList() {
        return idxList;
    }

    @Override
    public String toString() {
        String ret = name + " = getelementptr inbounds " + pointer.getType().getBasicType() + ", " + pointer.getType() + " "
                + pointer.getName() + ", ";
        for(int i = 0; i < idxList.size(); i++){
            ret += idxList.get(i).getType() + " " + idxList.get(i).getName();
            if(i != idxList.size() - 1)
                ret += ", ";
        }
        return  ret;
    }
}
