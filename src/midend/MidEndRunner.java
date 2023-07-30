package midend;

import ir.Function;
import ir.GlobalValue;

import java.util.ArrayList;
import java.util.HashMap;

public class MidEndRunner {
    private HashMap<String, Function> functions;
    private ArrayList<GlobalValue> globals;
    private boolean opt;

    public MidEndRunner(HashMap<String, Function> functions, ArrayList<GlobalValue> globals, boolean opt){
        this.functions = functions;
        this.globals = globals;
        this.opt = opt;
    }

    public void run(){

        if(opt){

        }
        return;
    }
}
