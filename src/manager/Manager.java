package manager;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import ir.Function;
import ir.GlobalValue;
import util.OutputHandler;

public class Manager {
    private static final Manager manager = new Manager();
    private static final HashMap<String, Function> functions = new HashMap<>();
    private static final ArrayList<GlobalValue> globals = new ArrayList<>();

    private Manager() {
    }

    public static ArrayList<GlobalValue> getGlobals() {
        return globals;
    }

    public static Manager getManager() {
        return manager;
    }

    public void addFunction(Function function) {
        functions.put(function.getName(), function);
    }

    public void addGlobal(GlobalValue value) {
        globals.add(value);
    }

    public boolean hasFunction(String name) {
        return functions.containsKey(name);
    }

    public static HashMap<String, Function> getFunctions() {
        return functions;
    }

    static int outputLLVMCnt = 0;

    public void outputLLVM(OutputStream out) {
        OutputHandler.clearLlvmString();
        // 全局变量
        for (GlobalValue globalValue : globals) {
            OutputHandler.addLlvmString(globalValue.toString());
        }
        // 函数声明
        // for (Function function : functions.values()) {
        // OutputHandler.addLlvmString(function.declare());
        // }
        // 函数定义
        for (Function function : functions.values()) {
            OutputHandler.addLlvmString(function.toString());
        }
        try {
            OutputHandler.outputLlvmString(out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void outputArm(OutputStream out){

    }
}
