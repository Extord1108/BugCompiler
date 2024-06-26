package ir;

import ir.instruction.Instr;
import ir.type.Type;
import manager.Manager;
import util.MyList;

import java.util.ArrayList;

public class Function extends Value {
    private ArrayList<Param> params;
    private MyList<BasicBlock> basicBlocks = new MyList<>();

    public Function(String name, ArrayList<Param> params, Type type) {
        this.name = name;
        this.params = params;
        this.type = type;
    }

    public boolean isExternal(){
        return Manager.getExternalFunctions().containsValue(this);
    }

    public static class Param extends Value {
        public String paramName;
        public Param(String name, Type type) {
            this.name = "%f" + Instr.getCount();
            this.paramName = name;
            this.type = type;
        }

        public Param( Type type) {
            this.name = "";
            this.type = type;
        }

        public String getParamName() {
            return paramName;
        }

        @Override
        public String toString() {
            return type + " " + name;
        }
    }

    public MyList<BasicBlock> getBasicBlocks() {
        return basicBlocks;
    }

    public ArrayList<Param> getParams() {
        return params;
    }

    public void addAtBegin(BasicBlock basicBlock) {
        basicBlocks.insertHead(basicBlock);
    }

    public void addAtEnd(BasicBlock basicBlock) {
        basicBlocks.insertTail(basicBlock);
    }

    public static ArrayList<Param> packParamTypes(Type... types) {
        ArrayList<Param> params = new ArrayList<>();
        for (Type type : types) {
            params.add(new Param(type));
        }
        return params;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("define dso_local ").append(type.toString()).append(" @").append(name).append("(");
        ArrayList<String> paramList = new ArrayList<>();
        if (params == null) {
            sb.append(") \n{\n");
        } else {
            for (Param param : params) {
                paramList.add(param.getType().toString() + " " + param.getName());
            }
            sb.append(String.join(", ", paramList)).append(") \n{\n");

        }
        for (BasicBlock basicBlock : basicBlocks) {
            sb.append(basicBlock.toString());
        }
        sb.append("}\n");
        return sb.toString();
    }

    public String declare() {
        StringBuilder sb = new StringBuilder();
        sb.append("declare ").append(type.toString()).append(" @").append(name).append("(");
        ArrayList<String> paramList = new ArrayList<>();
        if (params == null) {
            sb.append(")\n");
            return sb.toString();
        }
        for (Param param : params) {
            paramList.add(param.getType().toString()+" "+ param.getName());
        }
        sb.append(String.join(", ", paramList)).append(")\n");
        return sb.toString();
    }
}
