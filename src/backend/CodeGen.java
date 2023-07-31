package backend;

import frontend.semantic.OpTree;
import ir.*;
import ir.instruction.Binary;
import ir.instruction.Instr;
import lir.McBlock;
import lir.McFunction;
import lir.Operand;
import lir.mcInstr.McMove;
import manager.Manager;
import util.MyList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class CodeGen {
    public static boolean isOptMulDiv = true;

    private static final HashMap<String, Function> functions = Manager.getFunctions();
    private static final ArrayList<GlobalValue> globals = Manager.getGlobals();
    public static final CodeGen Instance = new CodeGen();
    private static final HashMap<Function, McFunction> funcMap = new HashMap<>();
    private static final HashMap<BasicBlock, McBlock> blockMap = new HashMap<>();
    private ArrayList<McFunction> mcFunctions = new ArrayList<>();
    private static Function curFunc;
    private static McFunction curMcFunc;
    private McBlock curMcBlock;

    private HashMap<Value, Operand> value2opd = new HashMap<>();


    private CodeGen(){
    }

    public void gen(){
        globalGen();

        for(Function function: functions.values()){
            McFunction mcFunction = new McFunction(function);
            funcMap.put(function, mcFunction);
        }
        for(Function function: functions.values()){
            if(function.isExternal())
                continue;
            curMcFunc = funcMap.get(function);
            mcFunctions.add(curMcFunc);
            curFunc = function;
            boolean isMain = false;
            if(curFunc.getName().equals("main")){
                isMain = true;
                curMcFunc.isMain = true;
            }
            MyList<BasicBlock> bList = curFunc.getBasicBlocks();
            Iterator<BasicBlock> iter = bList.iterator();
            // 实现构造出McBlock,使得其能被跳转指令找到
            while(iter.hasNext()){
                BasicBlock basicBlock = iter.next();
                McBlock mcBlock = new McBlock(basicBlock);
                blockMap.put(basicBlock, mcBlock);
            }
            iter = bList.iterator();
            while (iter.hasNext()){
                BasicBlock basicBlock = iter.next();
                genBasicBlock(basicBlock);
            }
        }
    }

    public void genBasicBlock(BasicBlock basicBlock){
        curMcBlock = blockMap.get(basicBlock);
        curMcBlock.setMcFunction(curMcFunc);
        MyList<Instr> instrs = basicBlock.getInstrs();
        for(Instr instr:instrs){
            if(instr instanceof Binary){
                genBinaryInstr((Binary) instr);
            }

        }
    }

    public void genBinaryInstr(Binary instr){
        Value left = instr.getUse(0);
        Value right = instr.getUse(1);
        OpTree.Operator op = instr.getOp();
        switch (op){
            case Mod -> {
                boolean isPower2 = false;
                int imm = 1, abs = 1;
                if(right instanceof Variable.ConstInt){
                    imm = ((Variable.ConstInt)right).getIntVal();
                    abs = Math.abs(imm);
                    if((abs & (abs - 1)) == 0){
                        isPower2 = true;
                    }
                }
                if(isOptMulDiv && isPower2){
                    assert abs != 0;
                    if(left instanceof Variable.ConstInt){
                        int valLeft = ((Variable.ConstInt)left).getIntVal();
                        new McMove(curMcBlock);
                    }
                }
            }
        }
    }

    public void globalGen(){
        System.err.println("暂时不确定是否需要globalGen，有待进一步考虑，暂时不写");
//        for(GlobalValue globalValue: globals){
//            assert globalValue.getType() instanceof PointerType;
//
//        }
    }

    public Operand getOperand(Value value){
        Operand opd = value2opd.get(value);
        if(opd == null){

        }
        return opd;
    }
}
