package manager;

import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

import frontend.semantic.InitVal;
import ir.Function;
import ir.GlobalValue;
import ir.Variable;
import ir.type.FloatType;
import ir.type.Int32Type;
import ir.type.PointerType;
import ir.type.VoidType;
import lir.McFunction;
import lir.Operand;
import util.OutputHandler;

public class Manager {
    private static final HashMap<String, Function> functions = new HashMap<>();
    private static final HashMap<String, Function> externalFunctions = new HashMap<>();
    private static final ArrayList<McFunction> mcFunclist = new ArrayList<>();
    private static final ArrayList<GlobalValue> globals = new ArrayList<>();
    private static final ArrayList<Operand.Global> globalOpds = new ArrayList<>();
    private static final ArrayList<Operand.Global> bssGlobals = new ArrayList<>();
    private static final ArrayList<Operand.Global> dataGlobals = new ArrayList<>();
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
        public static final Function SYSY_START_TIME = new Function( "_sysy_starttime", Function.packParamTypes(Int32Type.getInstance()), VoidType.getInstance());
        public static final Function SYSY_STOP_TIME = new Function("_sysy_stoptime", Function.packParamTypes(Int32Type.getInstance()), VoidType.getInstance());
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

    public static void addMcFunc(McFunction function) {
        mcFunclist.add(function);
    }

    public static void addGlobalOpds(Operand.Global global) {
        globalOpds.add(global);
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

    public static ArrayList<McFunction> getMcFunclist() {
        return mcFunclist;
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

    public void bssInit() {
        for(Operand.Global global: globalOpds) {
            InitVal initVal = global.getGlobalValue().getInitVal();
            if(initVal.getValue() instanceof Variable.ZeroInit) {
                bssGlobals.add(global);
                continue;
            }
            dataGlobals.add(global);
        }
    }

    public void outputArm(OutputStream out){
        OutputHandler.clearArmString();
        StringBuilder stb = new StringBuilder();
        stb.append(".arch armv7ve\n.arm\n");
        stb.append(".section .text\n");
        OutputHandler.addArmString(stb.toString());
        for(McFunction mcFunction: mcFunclist) {
            OutputHandler.addArmString(mcFunction.toString());
        }
        stb= new StringBuilder();
        stb.append("\n\n");
        stb.append(".section .bss\n");
        stb.append(".align 2\n");
        for(Operand.Global global: bssGlobals) {
            stb.append("\n.global\t").append(global).append("\n");
            stb.append(global).append(":\n");
            stb.append(global.getGlobalValue().getInitVal().armOut());
        }
        stb.append("\n.section .data\n");
        stb.append(".align 2\n");
        for(Operand.Global global: dataGlobals) {
            stb.append("\n.global\t").append(global).append("\n");
            stb.append(global).append(":\n");
            stb.append(global.getGlobalValue().getInitVal().armOut());
        }
        OutputHandler.addArmString(stb.toString());
        try {
            OutputHandler.outputArmString(out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
