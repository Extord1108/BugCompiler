package manager;

import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

import ir.Function;
import ir.GlobalValue;
import ir.type.FloatType;
import ir.type.Int32Type;
import ir.type.PointerType;
import ir.type.VoidType;
import util.OutputHandler;

public class Manager {
    private static final HashMap<String, Function> functions = new HashMap<>();
    private static final HashMap<String, Function> externalFunctions = new HashMap<>();
    private static final ArrayList<GlobalValue> globals = new ArrayList<>();
    private static final Manager manager = new Manager();

    public static class ExternFunction {
        public static final Function GET_INT = new Function( "getint", new ArrayList<>(), Int32Type.getInstance());
        public static final Function GET_CH = new Function( "getch", new ArrayList<>(), Int32Type.getInstance());
        public static final Function GET_FLOAT = new Function( "getfloat", new ArrayList<>(), FloatType.getInstance());
        public static final Function GET_ARR = new Function("getarray", Function.packParamTypes(new PointerType(Int32Type.getInstance())), Int32Type.getInstance());
        public static final Function GET_FARR = new Function("getfarray", Function.packParamTypes(new PointerType(FloatType.getInstance())), Int32Type.getInstance());
        public static final Function PUT_INT = new Function("putint", Function.packParamTypes(Int32Type.getInstance()), VoidType.getInstance());
        public static final Function PUT_CH = new Function("putch", Function.packParamTypes(Int32Type.getInstance()), VoidType.getInstance());
        public static final Function PUT_FLOAT = new Function("putfloat", Function.packParamTypes(FloatType.getInstance()), VoidType.getInstance());
        public static final Function PUT_ARR = new Function("putarray",Function.packParamTypes(Int32Type.getInstance(),new PointerType(Int32Type.getInstance())),VoidType.getInstance());
        public static final Function PUT_FARR = new Function("putfarray", Function.packParamTypes(Int32Type.getInstance(), new PointerType(FloatType.getInstance())), VoidType.getInstance());
        public static final Function MEM_SET = new Function("memset", Function.packParamTypes(new PointerType(Int32Type.getInstance()), Int32Type.getInstance(), Int32Type.getInstance()), VoidType.getInstance());
        public static final Function START_TIME = new Function( "starttime", Function.packParamTypes(Int32Type.getInstance()), VoidType.getInstance());
        public static final Function STOP_TIME = new Function("stoptime", Function.packParamTypes(Int32Type.getInstance()), VoidType.getInstance());
        public static final Function PARALLEL_START = new Function("parallel_start", new ArrayList<>(), Int32Type.getInstance());
        public static final Function PARALLEL_END = new Function("parallel_end", Function.packParamTypes(Int32Type.getInstance()), VoidType.getInstance());
    }

    private Manager() {
        addExternalFunctions();
    }

    public static ArrayList<GlobalValue> getGlobals() {
        return globals;
    }

    public static Manager getManager() {
        return manager;
    }

    public static HashMap<String, Function> getExternalFunctions() {
        return externalFunctions;
    }

    public void addFunction(Function function) {
        functions.put(function.getName(), function);
    }

    private void addExternalFunctions() {
        for (Field field : ExternFunction.class.getDeclaredFields()) {
            try {
                Function function = (Function) field.get(ExternFunction.class);
                externalFunctions.put(function.getName(), function);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        functions.putAll(externalFunctions);
        // externalFunctions.put(ExternFunction.GET_INT.getName(), ExternFunction.GET_INT);
        // externalFunctions.put(ExternFunction.GET_CH.getName(), ExternFunction.GET_CH);
        // externalFunctions.put(ExternFunction.GET_FLOAT.getName(), ExternFunction.GET_FLOAT);
        // externalFunctions.put(ExternFunction.GET_ARR.getName(), ExternFunction.GET_ARR);
        // externalFunctions.put(ExternFunction.GET_FARR.getName(), ExternFunction.GET_FARR);
        // externalFunctions.put(ExternFunction.PUT_INT.getName(), ExternFunction.PUT_INT);
        // externalFunctions.put(ExternFunction.PUT_CH.getName(), ExternFunction.PUT_CH);
        // externalFunctions.put(ExternFunction.PUT_FLOAT.getName(), ExternFunction.PUT_FLOAT);
        // externalFunctions.put(ExternFunction.PUT_ARR.getName(), ExternFunction.PUT_ARR);
        // externalFunctions.put(ExternFunction.PUT_FARR.getName(), ExternFunction.PUT_FARR);
        // externalFunctions.put(ExternFunction.MEM_SET.getName(), ExternFunction.MEM_SET);
        // externalFunctions.put(ExternFunction.START_TIME.getName(), ExternFunction.START_TIME);
        // externalFunctions.put(ExternFunction.STOP_TIME.getName(), ExternFunction.STOP_TIME);
        // externalFunctions.put(ExternFunction.PARALLEL_START.getName(), ExternFunction.PARALLEL_START);
        // externalFunctions.put(ExternFunction.PARALLEL_END.getName(), ExternFunction.PARALLEL_END);
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
        //函数声明
        for (Function function : externalFunctions.values()) {
        OutputHandler.addLlvmString(function.declare());
        }
        // 函数定义
        for (Function function : functions.values()) {
            if(externalFunctions.containsKey(function.getName())) continue;
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
