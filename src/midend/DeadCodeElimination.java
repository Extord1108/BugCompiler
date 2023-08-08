package midend;
import ir.*;
import ir.instruction.*;
import midend.Pass;
import util.MyList;

import java.util.*;

public class DeadCodeElimination extends Pass {
    public DeadCodeElimination(HashMap<String, Function> functions, ArrayList<GlobalValue> globals) {
        super(functions, globals);
    }

    @Override
    void run() {
        new UselessReturnElimination(functions, globals).run();//删除没有用的return
        new PhiFunctionPruning(functions, globals).run();//删除不活跃的phi指令
        new UselessFunctionElimination(functions, globals).run();//删除没有用的函数
        new ADCE(functions, globals).run();//激进的死代码删除
    }

    private class UselessReturnElimination extends Pass{
        public UselessReturnElimination(HashMap<String, Function> functions, ArrayList<GlobalValue> globals) {
            super(functions, globals);
        }
        @Override
        public void run(){
            for(Function function : this.functions.values()) {
                if(function.isExternal()|| function.getName().equals("main")) {
                    continue;
                }
                boolean canRemove = true;
                for(Used used:function.getUsedInfo()){
                    Instr call = used.getUser();
                    for(Used callUsed:call.getUsedInfo()){
                        Instr user = callUsed.getUser();
                        if(!(user instanceof Return && user.getBasicBlock().getFunction().equals(function))){
                            canRemove = false;
                            break;
                        }
                    }
                }
                if(canRemove){
                    for(BasicBlock bb:function.getBasicBlocks()){
                        for(Instr instr:bb.getInstrs()){
                            if(instr instanceof Return){
                                if(instr.getUses().size()>0)
                                instr.replaceUse(instr.getUse(0),Variable.getDefaultZero(function.getType()));
                            }
                        }
                    }
                }
            }
        }
    }

    private class PhiFunctionPruning extends Pass{

        public PhiFunctionPruning(HashMap<String, Function> functions, ArrayList<GlobalValue> globals) {
            super(functions, globals);
        }
        @Override
        public void run(){
            for(Function function : this.functions.values()) {
                if(function.isExternal()) {
                    continue;
                }
                prunePhiFunction(function);
            }
        }
        private void prunePhiFunction(Function function){
            HashMap<Instr,Boolean> useful = new HashMap<>();
            Stack<Instr> stack = new Stack<>();
            for(BasicBlock bb : new ReversePostOrder(function).get()){
                for(Instr instr : bb.getInstrs()){
                    if(instr instanceof Phi){
                        Phi phi = (Phi) instr;
                        useful.put(phi,false);
                    }else{
                        for(Value use: instr.getUses()){
                            if(use instanceof Phi){
                                stack.push((Instr) use);
                                useful.put((Instr) use,true);
                            }
                        }
                    }
                }
            }

            while(!stack.isEmpty()){
                Instr instr = stack.pop();
                for(Value use: instr.getUses()){
                    if(use instanceof Phi){
                        if(!useful.get(use)){
                            stack.push((Instr) use);
                            useful.put((Instr) use,true);
                        }
                    }
                }
            }
            for(BasicBlock bb : function.getBasicBlocks()){
                for(Instr instr : bb.getInstrs()){
                    if(instr instanceof Phi){
                        if(!useful.get(instr)){
                            instr.remove();
                        }
                    }
                }
            }
        }
    }

    private class ADCE extends Pass {
        HashSet<Value> useful = new HashSet<>();
        LinkedList<Value> queue = new LinkedList<>();
        public ADCE(HashMap<String, Function> functions, ArrayList<GlobalValue> globals) {
            super(functions, globals);
        }

        @Override
        void run() {
            for(Function function : this.functions.values()) {
                for(BasicBlock bb : function.getBasicBlocks()){
                    for(Instr instr : bb.getInstrs()){
                        if(instr.getUsedInfo().size()!=0 || instr instanceof Call || instr instanceof Return || instr instanceof Branch || instr instanceof Jump || instr instanceof Store){
                            queue.addLast(instr);
                        }
                    }
                    while (!queue.isEmpty()){
                        setUseful(queue.pop());
                    }
                }
            }
            for(Function function : this.functions.values()) {
                if(function.isExternal()) {
                    continue;
                }
                for(BasicBlock bb : function.getBasicBlocks()){
                    if(!useful.contains(bb)){
                        bb.remove();
                    } else{
                        for(Instr instr : bb.getInstrs()){
                            if(!useful.contains(instr)){
                                instr.remove();
                            }
                        }
                    }
                }
            }
        }

        private void setUseful(Value value){
            if(useful.contains(value))
                return;
            useful.add(value);
            if(value instanceof Instr){
                useful.add(((Instr) value).getBasicBlock());
            }
            if(value instanceof Instr){
                for(Value use:((Instr) value).getUses()){
                    if((use instanceof Instr || use instanceof Function || use instanceof BasicBlock) && !useful.contains(use)){
                        queue.addLast(use);
                    }
                }
            }
        }
    }

    private class UselessFunctionElimination extends Pass {
        public UselessFunctionElimination(HashMap<String, Function> functions, ArrayList<GlobalValue> globals) {
            super(functions, globals);
        }
        private HashSet<Function> visited = new HashSet<>();
        private HashSet<Function> uselessFunctions = new HashSet<>();

        @Override
        void run() {
            eliminateUselessFunctions();
        }
        private boolean findUselessFunctions(Function function) {
            visited.add(function);
            if(function.getUsedInfo().size() == 0)
                return false;
            for (BasicBlock bb : function.getBasicBlocks()) {
                for (Instr instr : bb.getInstrs()) {
                    if (instr instanceof Store) {
                        return false;
                    }
                    if (instr instanceof Call) {
                        Function callee = ((Call) instr).getFunction();
                        if(callee.equals(function))
                            return false;
                        if (visited.contains(callee)) {
                            if(!uselessFunctions.contains(callee)) {
                                return false;
                            }
                        } else {
                            if(!findUselessFunctions(callee)) {
                                return false;
                            }
                        }
                    }
                }
            }
            uselessFunctions.add(function);
            return true;
        }

        private void eliminateUselessFunctions() {
            for(Function function : this.functions.values()) {
                if(function.isExternal()) {
                    visited.add(function);
                }
                else{
                    findUselessFunctions(function);
                }
            }
            for(Function function : this.functions.values()) {
                if(function.getName().equals("main") || function.isExternal()) {
                    continue;
                }
                else{
                    boolean isUsed = false;
                    for(Used used : function.getUsedInfo()) {
                        assert used.getUser() instanceof Call;
                        if(((Call) used.getUser()).getUsedInfo().size()==0)
                            continue;
                        else{
                            isUsed = true;
                            break;
                        }
                    }
                    if(!isUsed) {
                        for(BasicBlock bb: function.getBasicBlocks()) {
                            for(Instr instr: bb.getInstrs()) {
                                if(instr instanceof Return && instr.equals(bb.getLast())) {
                                    if(instr.getUses().size()>0 && instr.getUse(0) instanceof  Call){
                                        Call call = (Call) instr.getUse(0);
                                        if(uselessFunctions.contains(call.getFunction()) && call.getUsedInfo().size()==1){
                                            MyList<Used> usedInfo = call.getUsedInfo();
                                            for(Used used : usedInfo){
                                                Value replaced = Variable.getDefaultZero(function.getType());
                                                call.repalceUseofMeto(replaced);
                                                call.remove();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


}