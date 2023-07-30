package backend;

import ir.Function;
import ir.GlobalValue;
import manager.Manager;

import java.util.ArrayList;
import java.util.HashMap;

public class CodeGen {
    private static final HashMap<String, Function> functions = Manager.getFunctions();
    private static final ArrayList<GlobalValue> globals = Manager.getGlobals();
    public static final CodeGen Instance = new CodeGen();


    private CodeGen(){
    }

    public void gen(){
        globalGen();
    }

    public void globalGen(){

    }

}
