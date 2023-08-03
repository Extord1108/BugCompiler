package midend;
import ir.*;
import ir.instruction.Call;
import ir.instruction.Instr;
import ir.instruction.Return;
import ir.instruction.Store;
import midend.Pass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class DeadCodeElimination extends Pass {
    public DeadCodeElimination(HashMap<String, Function> functions, ArrayList<GlobalValue> globals) {
        super(functions, globals);
    }

    @Override
    void run() {
    }


    private HashSet<Function> visited = new HashSet<>();
    private HashSet<Function> uselessFunctions = new HashSet<>();

    private boolean findUselessFunctions(Function function) {
        visited.add(function);
        for (BasicBlock bb : function.getBasicBlocks()) {
            for (Instr instr : bb.getInstrs()) {
                if (instr instanceof Call) {
                    Function callee = ((Call) instr).getFunction();
                    if (visited.contains(callee)) {
                        if(!uselessFunctions.contains(callee)) {
                            return false;
                        }
                    } else {
                        if(!findUselessFunctions(callee)) {
                            return false;
                        }
                    }
                } else if (instr instanceof Store) {
                    return false;
                }
            }
        }
        uselessFunctions.add(function);
        return true;
    }

    private void eliminateUselessFunctions() {
        for(Function function : this.functions.values()) {
            if(function.getName() == "main" || function.isExternal()) {
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
                for(Used used : function.getUsedInfo()) {
                    
                }
            }
        }
    }
}