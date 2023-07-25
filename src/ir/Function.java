package ir;

import ir.type.Type;
import util.MyList;

import java.util.ArrayList;
import java.util.HashMap;

public class Function extends Value {
    private HashMap<String, Type> params;
    private MyList<BasicBlock> basicBlocks = new MyList<>();

    public Function(String name, HashMap<String, Type> params, Type type) {
        this.name = name;
        this.params = params;
        this.type = type;
    }

    public void addAtBegin(BasicBlock basicBlock) {
        basicBlocks.insertHead(basicBlock);
    }

    public void addAtEnd(BasicBlock basicBlock) {
        basicBlocks.insertTail(basicBlock);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("define ").append(type.toString()).append(" @").append(name).append("(");
        ArrayList<String> paramList = new ArrayList<>();
        for (String paramName : params.keySet()) {
            paramList.add(params.get(paramName).toString() + " %" + paramName);
        }
        sb.append(String.join(", ", paramList)).append(") {");
        for (BasicBlock basicBlock : basicBlocks) {
            sb.append(basicBlock.toString());
        }
        sb.append("}");
        return sb.toString();
    }

    public String declare() {
        StringBuilder sb = new StringBuilder();
        sb.append("declare ").append(type.toString()).append(" @").append(name).append("(");
        ArrayList<String> paramList = new ArrayList<>();
        for (String paramName : params.keySet()) {
            paramList.add(params.get(paramName).toString() + " %" + paramName);
        }
        sb.append(String.join(", ", paramList)).append(")");
        return sb.toString();
    }
}
