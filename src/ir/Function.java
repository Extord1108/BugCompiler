package ir;

import ir.type.Type;
import util.MyList;

import java.util.ArrayList;
import java.util.HashMap;

public class Function extends Value{
    private HashMap<String, Type> params;
    private MyList<BasicBlock> basicBlocks = new MyList<>();

    public Function(String name, HashMap<String,Type> params, Type type){
        this.name = name;
        this.params = params;
        this.type = type;
    }

    public void addAtBegin(BasicBlock basicBlock){
        basicBlocks.insertHead(basicBlock);
    }

    public void addAtEnd(BasicBlock basicBlock){
        basicBlocks.insertTail(basicBlock);
    }
}
