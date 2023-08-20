package midend;

import ir.*;
import ir.instruction.*;
import ir.type.PointerType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class LocalArrayGVN extends Pass {


    private HashMap<String, Instr> GVNMap = new HashMap<>();
    private HashMap<String, Integer> GVNCount = new HashMap<>();

    public LocalArrayGVN(HashMap<String, Function> functions, ArrayList<GlobalValue> globals) {
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
        for(Function function:functions.values()){
            for(BasicBlock basicBlock:function.getBasicBlocks()){
                for(Instr instr:basicBlock.getInstrs()){
                    if(instr instanceof Alloc){
                        for(Used used:instr.getUsedInfo()){
                            DFS(instr,used.getUser());
                        }
                    }
                }
            }
            for(Function.Param param:function.getParams()){
                if(param.getType() instanceof  PointerType){
                    for(Used used:param.getUsedInfo()){
                        DFS(param,used.getUser());
                    }
                }
            }
        }
    }

    private void DFS(Value alloc, Instr instr){
        if (instr instanceof GetElementPtr) {
            for (Used used:instr.getUsedInfo()) {
                Instr user = used.getUser();
                DFS(alloc, user);
            }
        } else if (instr instanceof Load) {
            ((Load) instr).setAlloc(alloc);
            if (alloc instanceof Alloc) {
                ((Alloc) alloc).addLoad(instr);
            } else if (alloc instanceof Function.Param) {
                ((Function.Param) alloc).addLoad(instr);
            }
        } else if (instr instanceof Store) {
            ((Store) instr).setAlloc(alloc);
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
                Value alloc = ((Store) instr).getAlloc();
                if(alloc instanceof Alloc){
                    for(Instr load :((Alloc) alloc).getLoads()){
                        removeFromMap(load);
                    }
                }else{
                    for(Instr load :((Function.Param) alloc).getLoads()){
                        removeFromMap(load);
                    }
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
