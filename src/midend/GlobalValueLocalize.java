package midend;

import ir.*;
import ir.instruction.*;
import ir.type.FloatType;
import ir.type.Int32Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class GlobalValueLocalize extends Pass{

    private HashSet<GlobalValue> removedGlobal = new HashSet<>();
    public GlobalValueLocalize(HashMap<String, Function> functions, ArrayList<GlobalValue> globals) {
        super(functions, globals);
    }

    public void run(){
        for(GlobalValue value: globals) {
            if(value.initVal.getType() instanceof FloatType || value.initVal.getType() instanceof Int32Type) {
                localizeSingleValue(value);
            }
        }
        for(GlobalValue value: removedGlobal) {
            globals.remove(value);
        }
    }

    private void localizeSingleValue(GlobalValue value) {
        boolean hasStore = false;
        ArrayList<Function> useFunctions = new ArrayList<>();
        HashSet<Instr> useInstrs = new HashSet<>();
        for(Used used: value.getUsedInfo()) {
            Instr user = used.getUser();
            if(user instanceof Store) {
                hasStore = true;
            }
            useInstrs.add(user);
            if(!useFunctions.contains(user.getBasicBlock().getFunction()))
                useFunctions.add(user.getBasicBlock().getFunction());
        }
        if(!hasStore) {

            for (Instr instr : useInstrs) {
                Value initVal = value.getInitVal().getValue();
                assert initVal instanceof Variable.ConstInt || initVal instanceof Variable.ConstFloat;
                instr.repalceUseofMeto(initVal);
                instr.remove();
            }
            removedGlobal.add(value);
            return;
        }

        if(useFunctions.size() == 1) {
            Function function = useFunctions.get(0);
            if (!function.getName().equals("main")) {
                return;
            }
            if(canRemoveGlobal(function)) {
                BasicBlock entryBB = function.getEntryBlock();
                Value initVal = value.getInitVal().getValue();
                assert initVal instanceof Variable.ConstInt || initVal instanceof Variable.ConstFloat;
                if(initVal.getType() instanceof FloatType) {
                    Alloc alloc = new Alloc(FloatType.getInstance(), entryBB);
//                    System.out.println(alloc);
                    Store store = new Store(initVal, alloc, entryBB, false);
                    value.repalceUseofMeto(alloc);
                    entryBB.getInstrs().insertAfter(alloc, store);
                } else if(initVal.getType() instanceof Int32Type) {
                    Alloc alloc = new Alloc(Int32Type.getInstance(), entryBB);
//                    System.out.println(alloc);
                    Store store = new Store(initVal, alloc, entryBB, false);
//                    System.out.println(store);
                    value.repalceUseofMeto(alloc);
                    entryBB.getInstrs().insertAfter(alloc, store);
                } else{
                    assert false;
                }
                removedGlobal.add(value);
            }
        }
    }

    private boolean canRemoveGlobal(Function function){
        if(function.getUsedInfo().size() == 0) return true;
        for(BasicBlock bb : function.getBasicBlocks()){
            for(Instr instr:bb.getInstrs()){
                if(instr instanceof Call && ((Call)instr).getFunction().equals(function)) return false;
            }
        }
        return true;
    }
}
