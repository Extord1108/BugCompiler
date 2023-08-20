package ir;

import ir.instruction.Instr;
import ir.instruction.Load;
import ir.type.Type;
import manager.Manager;
import util.MyList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Function extends Value {
    private ArrayList<Param> params;
    private MyList<BasicBlock> basicBlocks = new MyList<>();

    //构造CFG
    private HashMap<BasicBlock, ArrayList<BasicBlock>> predMap = new HashMap<>();
    private HashMap<BasicBlock, ArrayList<BasicBlock>> succMap = new HashMap<>();
    private HashMap<BasicBlock,BasicBlock> domTree = new HashMap<>();
    private HashMap<BasicBlock,ArrayList<BasicBlock>> domFrontier = new HashMap<>();

    //GVN/GCM相关
    private boolean canGVN = false;

    public Function(String name, ArrayList<Param> params, Type type) {
        this.name = name;
        this.params = params;
        this.type = type;
    }

    public boolean isExternal(){
        return Manager.getExternalFunctions().containsValue(this);
    }

    public static class Param extends Value {
        private HashSet<Instr> loads = new HashSet<>();
        public String paramName;
        Param cloneParam = null;
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

        public Param clone() {
            cloneParam = new Param(paramName, type);
            return cloneParam;
        }

        public Param getClone() {
            return cloneParam;
        }

        public void cleanClone(){
            this.cloneParam = null;
        }

        public void cleanLoad(){
            this.loads.clear();
        }

        public HashSet<Instr> getLoads(){
            return this.loads;
        }

        public void addLoad(Instr load){
            this.loads.add(load);
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

    public BasicBlock getEntryBlock() {
//        System.out.println(name);
        return basicBlocks.get(0);
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

    public HashMap<BasicBlock, ArrayList<BasicBlock>> getPredecessors() {
        return predMap;
    }

    public HashMap<BasicBlock, ArrayList<BasicBlock>> getSuccessors() {
        return succMap;
    }

    public void setPredecessors(HashMap<BasicBlock, ArrayList<BasicBlock>> predecessors) {
        this.predMap = predecessors;
    }

    public void setSuccessors(HashMap<BasicBlock, ArrayList<BasicBlock>> successors) {
        this.succMap = successors;
    }

    public HashMap<BasicBlock, BasicBlock> getDomTree() {
        return domTree;
    }

    public void setDomTree(HashMap<BasicBlock, BasicBlock> domTree) {
        this.domTree = domTree;
    }

    public HashMap<BasicBlock, ArrayList<BasicBlock>> getDomFrontier() {
        return domFrontier;
    }

    public void setDomFrontier(HashMap<BasicBlock, ArrayList<BasicBlock>> domFrontier) {
        this.domFrontier = domFrontier;
    }

    public void setCanGVN(boolean canGVN) {
        this.canGVN = canGVN;
    }

    public boolean canGVN() {
        return canGVN;
    }

    public void cleanDomInfo(){
        domTree.clear();
        domFrontier.clear();
        succMap.clear();
        predMap.clear();
        for(BasicBlock basicBlock:basicBlocks){
            basicBlock.cleanDomInfo();
        }
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
