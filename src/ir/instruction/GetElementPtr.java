package ir.instruction;

import ir.BasicBlock;
import ir.Value;
import ir.type.Type;

import java.util.ArrayList;

public class GetElementPtr extends Instr{

    Value pointer;
    ArrayList<Value> idxList;

    public GetElementPtr(Type type, Value pointer, ArrayList<Value> idxList, BasicBlock basicBlock) {
        super(type, basicBlock);
        this.pointer = pointer;
        this.idxList = idxList;
    }

    @Override
    public String toString() {
        return "请补充GetElementPtr的toString";
    }
}
