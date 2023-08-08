package midend;

import ir.Function;
import ir.GlobalValue;

import java.util.ArrayList;
import java.util.HashMap;

public class GCM extends Pass {

    public GCM(HashMap<String, Function> functions, ArrayList<GlobalValue> globals) {
        super(functions, globals);
    }
}
