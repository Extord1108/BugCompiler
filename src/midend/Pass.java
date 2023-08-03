package midend;

import ir.Function;
import ir.GlobalValue;

import java.util.ArrayList;
import java.util.HashMap;

public class Pass {
    HashMap<String, Function> functions;
    ArrayList<GlobalValue> globals;
    public Pass( HashMap<String, Function> functions, ArrayList<GlobalValue> globals) {
        this.functions = functions;
        this.globals = globals;
    }

    void run(){

    }
}
