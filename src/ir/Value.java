package ir;

import ir.type.Type;
import util.MyList;
import util.MyNode;

import java.util.ArrayList;

public class Value extends MyNode {
    public Type type;
    public String name;
    public MyList<Used> usedInfo = new MyList<>(); //使用当前value的相关信息

    public Value(){

    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public MyList<Used> getUsedInfo() {
        return usedInfo;
    }

    public void addUsed(Used used){
        usedInfo.insertTail(used);
    }
}
