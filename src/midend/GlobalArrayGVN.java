package midend;

import ir.*;
import ir.instruction.*;
import ir.type.ArrayType;
import ir.type.PointerType;

import java.util.ArrayList;
import java.util.HashMap;

public class GlobalArrayGVN extends Pass{
    private HashMap<String, Instr> GVNMap = new HashMap<>();
    private HashMap<String, Integer> GVNCount = new HashMap<>();

    public GlobalArrayGVN(HashMap<String, Function> functions, ArrayList<GlobalValue> globals) {
        super(functions, globals);
    }

    public void run(){
        init();
        for(Function function:functions.values()){
            if(function.isExternal()) continue;
            runForFunc(function);
        }
    }

    public void init(){
        for (Function function:functions.values()){
            for(BasicBlock basicBlock:function.getBasicBlocks()){
                for(Instr instr:basicBlock.getInstrs()){
                    if(instr instanceof Alloc){
                        ((Alloc) instr).cleanLoad();
                    }else if(instr instanceof Load){
                        ((Load) instr).cleanAS();
                    }else if(instr instanceof Store){
                        ((Store) instr).cleanAL();
                    }
                }
            }
            for(Function.Param param:function.getParams()){
                if(param.getType() instanceof PointerType){
                    param.cleanLoad();
                }
            }
        }
        for(GlobalValue gval:globals){
            if(gval.getType().getBasicType() instanceof ArrayType){
                for(Used used:gval.getUsedInfo()){
                    DFS(gval,used.getUser());
                }
            }
        }
    }

    private void DFS(Value gval, Instr instr){
        if (instr instanceof GetElementPtr) {
            for (Used used:instr.getUsedInfo()) {
                DFS(gval, used.getUser());
            }
        } else if (instr instanceof Load) {
            ((Load) instr).setAlloc(gval);
            ((GlobalValue)gval).addLoad(instr);
        } else if (instr instanceof Store) {
            ((Store) instr).setAlloc(gval);
        }
    }

    private void runForFunc(Function function){
        BasicBlock entryBlock = function.getBasicBlocks().get(0);
        RPOSearch(entryBlock);
    }

    private void RPOSearch(BasicBlock bb){
        for(Instr instr:bb.getInstrs()){
            if(instr instanceof Load && ((Load) instr).getAlloc()!=null){
                addToMap(instr);
            }else if(instr instanceof Store && ((Store) instr).getAlloc()!=null){
                Value gval = ((Store) instr).getAlloc();
                for(Instr load :((GlobalValue) gval).getLoads()){
                    removeFromMap(load);
                }
            }
        }

        for(BasicBlock next:bb.getIDoms()){
            RPOSearch(next);
        }
    }

    private boolean addToMap(Instr instr){
        String key = getHashofInstr(instr);
        if(GVNMap.containsKey(key))
        {
            instr.repalceUseofMeto(GVNMap.get(key));
            return false;
        }
        else{
            if(!GVNCount.containsKey(key))
                GVNCount.put(key, 1);
            else
                GVNCount.put(key, GVNCount.get(key)+1);
            if(!GVNMap.containsKey(key))
                GVNMap.put(key, instr);
            return true;
        }
    }

    private void removeFromMap(Instr instr){
        String key = getHashofInstr(instr);
        if (GVNCount.containsKey(key) && GVNCount.get(key) > 0) {
            GVNCount.put(key, GVNCount.get(key) - 1);
            if (GVNCount.get(key) == 0) {
                GVNMap.remove(key);
            }
        }
    }

    private String getHashofInstr(Instr instr){
        if(instr instanceof Load){
            return "load"+((Load) instr).getPointer().getName();
        }
        return "";
    }
}
