package backend;

import ir.Function;
import ir.GlobalValue;
import ir.type.PointerType;
import lir.McFunction;
import manager.Manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CodeGen {
    private static final HashMap<String, Function> functions = Manager.getFunctions();
    private static final ArrayList<GlobalValue> globals = Manager.getGlobals();
    public static final CodeGen Instance = new CodeGen();
    private static final HashMap<Function, McFunction> funcMap = new HashMap<>();


    private CodeGen(){
    }

    public void gen(){
        globalGen();

        for(Function function: functions.values()){
            McFunction mcFunction = new McFunction(function);
            funcMap.put(function, mcFunction);
        }




    }

    public void globalGen(){
        System.err.println("暂时不确定是否需要globalGen，有待进一步考虑，暂时不写");
//        for(GlobalValue globalValue: globals){
//            assert globalValue.getType() instanceof PointerType;
//
//        }
    }

}
