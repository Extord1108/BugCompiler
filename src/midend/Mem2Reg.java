package midend;

import ir.Function;
import ir.GlobalValue;

import java.util.ArrayList;
import java.util.HashMap;

public class Mem2Reg extends Pass{
    public Mem2Reg(HashMap<String, Function> functions, ArrayList<GlobalValue> globals) {
        super(functions, globals);
    }

}
