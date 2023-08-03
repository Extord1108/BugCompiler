package midend;
import ir.*;
import ir.instruction.Call;
import ir.instruction.Instr;
import ir.instruction.Return;
import ir.instruction.Store;
import midend.Pass;
import util.MyList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

public class DeadCodeElimination extends Pass {
    public DeadCodeElimination(HashMap<String, Function> functions, ArrayList<GlobalValue> globals) {
        super(functions, globals);
    }

    @Override
    void run() {
        eliminateUselessFunctions();
    }


    private HashSet<Function> visited = new HashSet<>();
    private HashSet<Function> uselessFunctions = new HashSet<>();

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
            if(function.getName() == "main" || function.isExternal()) {
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
                                if(instr.getUse(0) instanceof  Call){
                                    Call call = (Call) instr.getUse(0);
                                    if(uselessFunctions.contains(call.getFunction()) && call.getUsedInfo().size()==1){
                                        MyList<Used> usedInfo = call.getUsedInfo();
                                        for(Used used : usedInfo){
                                            Value replaced = Variable.getDefaultZero(function.getType());
                                            used.getUser().replaceUse(used.getValue(), replaced);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        for (Function function : uselessFunctions) {
            System.out.println(function.getName());
        }
    }
}