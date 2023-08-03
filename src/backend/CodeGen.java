package backend;

import com.sun.jdi.FloatType;
import frontend.semantic.OpTree;
import ir.*;
import ir.instruction.*;
import ir.type.Int32Type;
import ir.type.PointerType;
import lir.McBlock;
import lir.McFunction;
import lir.Operand;
import lir.mcInstr.*;
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
    private HashMap<OpTree.Operator, Cond> icmpOp2cond = new HashMap<>();
    private HashMap<Value, Cond> value2cond = new HashMap<>();



    private CodeGen(){
    }

    public void gen(){
        initIcmpOp2cond();
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
            else if(instr instanceof Icmp || instr instanceof Fcmp) {
                genCmp(instr);
            }
            else if(instr instanceof Branch) {
                Value cond = ((Branch) instr).getCond();
                McBlock thenBlock = blockMap.get(((Branch) instr).getThenBlock());
                McBlock elseBlock = blockMap.get(((Branch) instr).getThenBlock());
                thenBlock.addPreMcBlock(curMcBlock);
                elseBlock.addPreMcBlock(curMcBlock);

            }
            else if(instr instanceof Jump) {
                McBlock targetBlock = blockMap.get(((Jump) instr).getTargetBlock());
                curMcBlock.
            }
            else if(instr instanceof BitCast) {
                Operand opd = getOperand(instr.getUse(0));
                value2opd.put(instr, opd);
            }
            else if(instr instanceof Alloc) {

            }
            else {
                System.err.println("存在ir类型" + instr.getClass() + "未被解析为lir");
            }
        }
    }

    public void genCmp(Instr instr) {
        Operand dst = getOperand(instr);
        Operand dstVr = getOperand(instr);
        if(instr instanceof Icmp){
            Icmp icmp = (Icmp) instr;
            OpTree.Operator op = icmp.getOp();
            Value left = icmp.getLhs();
            Value right = icmp.getRhs();
            Operand lopd = getOperand(left);
            Operand ropd = getOperand(right);
            if(lopd.isImm() && ropd.isImm()){
                int imm1 = ((Operand.Imm)lopd).getIntNumber();
                int imm2 = ((Operand.Imm)ropd).getIntNumber();
                boolean ans = false;
                ans = ((imm1 <= imm2 && op.equals(OpTree.Operator.Le)) ||
                        (imm1 < imm2 && op.equals(OpTree.Operator.Lt)) ||
                        (imm1 > imm2 && op.equals(OpTree.Operator.Gt)) ||
                        (imm1 >= imm2 && op.equals(OpTree.Operator.Ge)) ||
                        (imm1 != imm2 && op.equals(OpTree.Operator.Ne)) ||
                        (imm1 == imm2 && op.equals(OpTree.Operator.Eq)));
                Operand.Imm imm;
                if(ans){
                    imm = new Operand.Imm(1);
                }else {
                    imm = new Operand.Imm(0);
                }
                new McMove(dstVr, imm, curMcBlock);
            } else {
                Cond cond = icmpOp2cond.get(op);
                if(lopd.isImm()) {
                    Operand temp = lopd;
                    lopd = ropd;
                    ropd = temp;
                    switch (cond) {
                        case Ge -> cond = Cond.Le;
                        case Le -> cond = Cond.Ge;
                        case Gt -> cond = Cond.Lt;
                        case Lt -> cond = Cond.Gt;
                    }
                }
                McCmp mcCmp = new McCmp(cond, lopd, ropd, curMcBlock);
                if(icmp.getNext() instanceof Branch && icmp.getUsedSize() == 1
                        && icmp.getUser(0).equals(icmp.getNext())) {
                    value2cond.put(instr, cond);
                } else {
                    new McMove( cond, dstVr, new Operand.Imm(1),curMcBlock);
                    new McMove(getIcmpOppoCond(cond), dstVr, new Operand.Imm(0), curMcBlock);
                }
            }
        }
        else {
            assert instr instanceof Fcmp;
            Fcmp fcmp = (Fcmp) instr;
            OpTree.Operator op = fcmp.getOp();
            Value left = fcmp.getLhs();
            Value right = fcmp.getRhs();

            if(left instanceof Variable.ConstFloat && right instanceof Variable.ConstFloat) {
                float imm1 = ((Variable.ConstFloat) left).getFloatVal();
                float imm2 = ((Variable.ConstFloat) right).getFloatVal();
                boolean ans = false;
                ans = ((imm1 <= imm2 && op.equals(OpTree.Operator.Le)) ||
                        (imm1 < imm2 && op.equals(OpTree.Operator.Lt)) ||
                        (imm1 > imm2 && op.equals(OpTree.Operator.Gt)) ||
                        (imm1 >= imm2 && op.equals(OpTree.Operator.Ge)) ||
                        (imm1 != imm2 && op.equals(OpTree.Operator.Ne)) ||
                        (imm1 == imm2 && op.equals(OpTree.Operator.Eq)));
                if(ans) {
                    new McMove(dstVr, new Operand.Imm(1), curMcBlock);
                } else {
                    new McMove(dstVr, new Operand.Imm(0), curMcBlock);
                }
            } else {
                Operand lopd = getOperand(left);
                Operand ropd = getOperand(right);
                Cond cond = getFcmp2Cond(op);
                McCmp mcCmp = new McCmp(cond, lopd, ropd, curMcBlock);
                if(fcmp.getNext() instanceof Branch && fcmp.getUsedSize() == 1
                        && fcmp.getUser(0).equals(fcmp.getNext())) {
                    value2cond.put(instr, cond);
                } else {
                    new McMove( cond, dstVr, new Operand.Imm(1),curMcBlock);
                    new McMove(getFcmpOppoCond(cond), dstVr, new Operand.Imm(0), curMcBlock);
                }
            }
        }
    }

    public void genBinaryInstr(Binary instr){
        Value left = instr.getUse(0);
        Value right = instr.getUse(1);
        OpTree.Operator op = instr.getOp();
        Operand dstVr = getOperand(instr);
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
                        new McMove(dstVr, getOperand(new Variable.ConstInt(valLeft % imm)),curMcBlock);
                    } else if(abs  == 1){
                        new McMove(dstVr, getOperand(new Variable.ConstInt(0)), curMcBlock);
                    } else {
                        Operand sign = new Operand.VirtualReg(false, curMcFunc);
                        Operand lVr = getOperand(left);
                        new MCShift(sign, lVr, getOperand(new Variable.ConstInt(31)),MCShift.ShiftType.asr,curMcBlock);
                        Operand immOpd = getOperand(new Variable.ConstInt(abs - 1));
                        new McBinary(McBinary.BinaryType.And, dstVr, lVr, immOpd, curMcBlock );
                        int sh = Integer.numberOfTrailingZeros(abs);
                        new McBinary(McBinary.BinaryType.Or, dstVr, dstVr, sign,
                                new MCShift( MCShift.ShiftType.lsl, new Operand.Imm(sh)), curMcBlock);
                    }
                }
                else {
                    Operand lopd = getOperand(left);
                    Operand ropd = getOperand(right);
                    Operand dst1 = new Operand.VirtualReg(left instanceof Variable.ConstFloat, curMcFunc);
                    new McBinary(McBinary.BinaryType.Div, dst1, lopd, ropd, curMcBlock);
                    new McBinary(McBinary.BinaryType.Mul, dst1, dst1, ropd, curMcBlock);
                    new McBinary(McBinary.BinaryType.Sub, dst1, lopd, dst1, curMcBlock);
                }
            }
            case Mul  -> {
                if(left instanceof Variable.ConstInt && right instanceof Variable.ConstInt){
                    int ans = ((Variable.ConstInt) left).getIntVal() * ((Variable.ConstInt) right).getIntVal();
                    new McMove(dstVr, getOperand(new Variable.ConstInt(ans)),curMcBlock);
                } else if(left instanceof Variable.ConstInt || right instanceof Variable.ConstInt) {
                    Operand src;
                    int imm;
                    if(left instanceof Variable.ConstInt){
                        imm = ((Variable.ConstInt) left).getIntVal();
                        src = getOperand(right);
                    }
                    else{
                        imm = ((Variable.ConstInt) right).getIntVal();
                        src = getOperand(left);
                    }
                    int abs = Math.abs(imm);
                    if(abs == 0 && isOptMulDiv){
                        new McMove(dstVr,  new Operand.Imm( 0),curMcBlock);
                    }
                    else if((abs & (abs - 1)) == 0 && isOptMulDiv){
                        // 2的多少次方
                        int sh = 31 - Integer.numberOfLeadingZeros(abs);
                        if(sh == 0) {
                            new McMove(dstVr, src, curMcBlock);
                        } else {
                            new MCShift(dstVr, dstVr, getOperand(new Variable.ConstInt(sh)),MCShift.ShiftType.lsl,curMcBlock);
                        }
                        if(imm < 0) {
                            new McBinary(McBinary.BinaryType.Rsb, dstVr, dstVr, new Operand.Imm(0),curMcBlock);
                        }
                    }
                    else {
                        new McBinary(McBinary.BinaryType.Mul, dstVr, src, getOperand(new Variable.ConstInt(imm)), curMcBlock);
                    }
                } else {
                    // 没有常量的int类型乘法
                    Operand lopd = getOperand(left);
                    Operand ropd = getOperand(right);
                    new McBinary(McBinary.BinaryType.Mul, dstVr, lopd, ropd, curMcBlock);
                }
            }
            case Div -> {
                if(left instanceof Variable.ConstInt && right instanceof Variable.ConstInt){
                    int ans = ((Variable.ConstInt) left).getIntVal() / ((Variable.ConstInt) right).getIntVal();
                    new McMove(dstVr, getOperand(new Variable.ConstInt(ans)),curMcBlock);
                }
                else if(right instanceof Variable.ConstInt){
                    Operand lopd = getOperand(left);
                    int imm = ((Variable.ConstInt) right).getIntVal();
                    int abs = Math.abs(imm);
                    if(imm == 1){
                        new McMove(dstVr, lopd, curMcBlock);
                    } else if(imm == -1) {
                        new McBinary(McBinary.BinaryType.Rsb, dstVr, lopd,
                                new Operand.Imm(0), curMcBlock);
                    } else if((abs & (abs -1)) == 0){
                        int sh = 31 - Integer.numberOfLeadingZeros(abs);
                        Operand sign = new Operand.VirtualReg(false, curMcFunc);
                        Operand lVr = getOperand(left);
                        new MCShift(sign, lVr, getOperand(new Variable.ConstInt(31)),MCShift.ShiftType.asr,curMcBlock);
                        Operand tmp = new Operand.VirtualReg(false, curMcFunc);
                        new McBinary(McBinary.BinaryType.Add, tmp, lopd, sign,
                                new MCShift( MCShift.ShiftType.lsr, new Operand.Imm(32 - sh)), curMcBlock);
                        new MCShift(dstVr, tmp, getOperand(new Variable.ConstInt(sh)),MCShift.ShiftType.asr,curMcBlock);
                        if(imm < 0){
                            new McBinary(McBinary.BinaryType.Rsb, dstVr, dstVr, new Operand.Imm(0),curMcBlock);
                        }
                    } else {
                        // TODO 魔法数的除法
                        new McBinary(McBinary.BinaryType.Mul, dstVr, lopd, getOperand(new Variable.ConstInt(imm)), curMcBlock);
                    }
                }
                else {
                    Operand lopd, ropd;
                    if(left instanceof Variable.ConstInt) {
                        Operand imm = new Operand.Imm( ((Variable.ConstInt) left).getIntVal());
                        lopd = new Operand.VirtualReg( false,curMcFunc);
                        new McMove(lopd, imm, curMcBlock);
                    } else {
                        lopd = getOperand(left);
                    }
                    ropd = getOperand(right);
                    new McBinary(McBinary.BinaryType.Div, dstVr, lopd, ropd, curMcBlock);
                }
            }
            case Add -> {
                if(left instanceof Variable.ConstInt && right instanceof Variable.ConstInt){
                    int ans = ((Variable.ConstInt) left).getIntVal() + ((Variable.ConstInt) right).getIntVal();
                    new McMove(dstVr, getOperand(new Variable.ConstInt(ans)),curMcBlock);
                } else {
                    if(left instanceof Variable.ConstInt) {
                        Value temp = left;
                        left = right;
                        right = temp;
                    }
                    Operand lopd = getOperand(left);
                    Operand ropd = getOperand(right);
                    new McBinary(McBinary.BinaryType.Add, dstVr, lopd, ropd, curMcBlock);
                }
            }
            case Sub -> {
                if(left instanceof Variable.ConstInt && right instanceof Variable.ConstInt){
                    int ans = ((Variable.ConstInt) left).getIntVal() - ((Variable.ConstInt) right).getIntVal();
                    new McMove(dstVr, getOperand(new Variable.ConstInt(ans)),curMcBlock);
                } else if(left instanceof Variable.ConstInt){
                    Operand ropd = getOperand(left);
                    Operand lopd = getOperand(right);
                    new McBinary(McBinary.BinaryType.Rsb, dstVr, lopd, ropd, curMcBlock);
                }else {
                    Operand lopd = getOperand(left);
                    Operand ropd = getOperand(right);
                    new McBinary(McBinary.BinaryType.Sub, dstVr, lopd, ropd, curMcBlock);
                }
            }
            default -> {
                System.err.println("尚未支持" + op + "的genBinaryInstr");
            }
        }
    }

    // global的虚拟寄存器需要实现声明，便于后期使用到的时候进行区分
    public void globalGen(){
//        System.err.println("暂时不确定是否需要globalGen，有待进一步考虑，暂时不写");
        for(GlobalValue globalValue: globals){
            assert globalValue.getType() instanceof PointerType;
            Operand opd = new Operand.Global(globalValue);
            value2opd.put(globalValue, opd);
        }
    }

    public Operand getOperand(Value value){
        Operand opd = value2opd.get(value);
        if(opd == null){
            if(value instanceof Variable.ConstInt){
                opd = getIntImm(value);
            }
            else if(value instanceof Variable.ConstFloat) {
                float floatVal = ((Variable.ConstFloat)value).getFloatVal();
                opd = getFloatImm(value);
            }
            else {
                if(value.getType() instanceof FloatType) {
                    opd = new Operand.VirtualReg(true, curMcFunc);
                } else {
                    opd = new Operand.VirtualReg(false, curMcFunc);
                }
                value2opd.put(value, opd);
            }
        }
        return opd;
    }

    public Operand getIntImm(Value value){
        int intVal = ((Variable.ConstInt)value).getIntVal();
        Operand opd = new Operand.Imm( intVal);
        if(!canImmSaved(intVal)){
            Operand dst = new Operand.VirtualReg(false, curMcFunc);
            new McMove(dst, opd, curMcBlock);
            opd = dst;
        }
        return opd;
    }

    public Operand getFloatImm(Value value) {
        float f = ((Variable.ConstFloat)value).getFloatVal();
        Operand dst = new Operand.VirtualReg(true, curMcFunc);
        if(canFImmSaved(f)) {
            Operand imm = new Operand.Imm(f);
            new McMove(dst, imm, curMcBlock);
        } else {
            Operand tmp;
            int bitFloat = Float.floatToRawIntBits(f);
            Operand imm = getIntImm(new Variable.ConstInt(bitFloat));
            if(canImmSaved(bitFloat)){
                tmp = new Operand.VirtualReg(false, curMcFunc);
                new McMove(tmp, imm, curMcBlock);
            } else {
                tmp = imm;
            }
            new McMove(dst, tmp, curMcBlock);
        }
        return dst;
    }

    // arm有一套神奇的Int的Imm是否能进行使用的机制
    public static boolean canImmSaved(int imm) {
        int n = imm;
        for (int i = 0; i < 32; i += 2) {
            if ((n & ~0x00ff) == 0) {
                return true;
            }
            n = (n << 30) | (n >> 2);
        }
        return false;
    }

    // arm神奇的Float的Imm是否能进行使用的机制
    public static boolean canFImmSaved(float imm) {
        float num = imm * 128;
        int compare;
        for (int i = 0; i < 8; i++) {
            for (int j = 16; j < 32; j++) {
                compare = j * ( 1 << i);
                if (Math.abs(num - compare) < 1e-14 || Math.abs(num + compare) < 1e-14) {
                    return true;
                }
            }
        }
        return false;
    }

    public void initIcmpOp2cond(){
        icmpOp2cond.put(OpTree.Operator.Eq, Cond.Eq);
        icmpOp2cond.put(OpTree.Operator.Ne, Cond.Ne);
        icmpOp2cond.put(OpTree.Operator.Ge, Cond.Ge);
        icmpOp2cond.put(OpTree.Operator.Gt, Cond.Gt);
        icmpOp2cond.put(OpTree.Operator.Lt, Cond.Lt);
        icmpOp2cond.put(OpTree.Operator.Le, Cond.Le);
    }

    public Cond getFcmp2Cond(OpTree.Operator op) {
        return switch (op){
            case Eq -> Cond.Eq;
            case Ne -> Cond.Ne;
            case Gt -> Cond.Hi;
            case Ge -> Cond.Pl;
            case Lt -> Cond.Lt;
            case Le -> Cond.Le;
            default -> null;
        };
    }

    public Cond getIcmpOppoCond(Cond cond){
        return switch (cond) {
            case Eq -> Cond.Ne;
            case Ne -> Cond.Eq;
            case Ge -> Cond.Lt;
            case Gt -> Cond.Le;
            case Le -> Cond.Gt;
            case Lt -> Cond.Ge;
            case Hi, Pl, Any -> throw new AssertionError("Wrong Icmp oppo cond");
        };
    }

    public Cond getFcmpOppoCond(Cond cond){
        return switch (cond) {
            case Eq -> Cond.Ne;
            case Ne -> Cond.Eq;
            case Hi -> Cond.Le;
            case Pl -> Cond.Lt;
            case Le -> Cond.Hi;
            case Lt -> Cond.Pl;
            case Ge, Gt, Any -> throw new AssertionError("Wrong Fcmp oppo cond");
        };
    }
}
