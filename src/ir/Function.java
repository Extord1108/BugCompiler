package ir;

import ir.type.Type;

import java.util.ArrayList;
import java.util.HashMap;

public class Function extends Value{
    private HashMap<String, Type> params;

    public Function(String name, HashMap<String,Type> params, Type type){
        this.name = name;
        this.params = params;
        this.type = type;
    }
}
