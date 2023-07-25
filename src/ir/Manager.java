package ir;

import java.util.ArrayList;
import java.util.HashMap;

public class Manager {
    private static final Manager manager = new Manager();
    private static final HashMap<String, Function> functions = new HashMap<>();
    private static final ArrayList<GlobalValue> globals = new ArrayList<>();



    private Manager(){
    }

    public static Manager getManager() {
        return manager;
    }

    public void addFunction(Function function){
        functions.put(function.getName(), function);
    }

    public void addGlobal(GlobalValue value){
        globals.add(value);
    }

    public boolean hasFunction(String name){
        return functions.containsKey(name);
    }

    public static HashMap<String, Function> getFunctions() {
        return functions;
    }
}
