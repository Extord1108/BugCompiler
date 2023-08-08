package backend;

import frontend.semantic.OpTree;
import ir.*;
import ir.instruction.*;
import ir.type.*;
import lir.McBlock;
import lir.McFunction;
import lir.Operand;
import lir.mcInstr.*;
import manager.Manager;
import util.MyList;

import java.util.*;

public class CodeGen {
    public static boolean isOptMulDiv = true;

    private static final HashMap<String, Function> functions = Manager.getFunctions();
    private static final ArrayList<GlobalValue> globals = Manager.getGlobals();
    public static final CodeGen Instance = new CodeGen();
    private static final HashMap<Function, McFunction> funcMap = new HashMap<>();
    private static final HashMap<BasicBlock, McBlock> blockMap = new HashMap<>();
    private static Function curFunc;
    private static McFunction curMcFunc;
    private McBlock curMcBlock;

    private HashMap<Value, Operand> value2opd = new HashMap<>();
    private HashMap<OpTree.Operator, Cond> icmpOp2cond = new HashMap<>();
    private HashMap<Value, Cond> value2cond = new HashMap<>();
    private HashMap<Value, Operand.Imm> value2Imm = new HashMap<>();

    HashSet<McBlock> visitBBset = new HashSet<>();
    Stack<BasicBlock> nextBBList = new Stack<>();

    // 整数数传参可使用最大个数
    public static final int rParamCnt = 4;
    // 浮点数传参可使用最大个数
    public static final int sParamCnt = 16;


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
            Manager.addMcFunc(curMcFunc);
            curFunc = function;
            if(curFunc.getName().equals("main")){
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
            // 不直接进行遍历而是写栈的主要原因是因为br的时候llvm可以写true和false，但是好像arm不能了，得处理处理块的顺序
            BasicBlock basicBlock = curFunc.getEntryBlock();
            curMcBlock = blockMap.get(basicBlock);
            curMcBlock.setMcFunction(curMcFunc);
            McBinary mcBinary = new McBinary(McBinary.BinaryType.Sub, Operand.PhyReg.getPhyReg("sp"),
                    Operand.PhyReg.getPhyReg("sp"), new Operand.Imm(0), curMcBlock);
            mcBinary.setNeedFix(McBinary.FixType.VAR_STACK);
            dealParam();

            nextBBList.push(basicBlock);
            while(nextBBList.size() > 0) {
                BasicBlock visitBlock = nextBBList.pop();
                genBasicBlock(visitBlock);
            }
        }
    }

    public void dealParam() {
        int sRegIdx = 0;    //  当前用到的浮点数寄存器下标
        int rRegIdx = 0;    //  当前用到的整数寄存器下标
        int regStack = 0;   //  溢出到栈上的寄存器数量
        for(Function.Param param: curFunc.getParams()) {
            Operand dst = getOperand(param);
            if(param.getType() instanceof ir.type.FloatType) {
                if(sRegIdx < sParamCnt) {
                    new McMove( dst, Operand.FPhyReg.getFPhyReg(sRegIdx), curMcBlock);
                    sRegIdx ++;
                } else {
                    int offset = -(regStack + 1) * 4;
                    Operand addr = new Operand.VirtualReg(false, curMcFunc);
                    McBinary mcBinary =new McBinary(McBinary.BinaryType.Add, addr, Operand.PhyReg.getPhyReg("sp"),
                            new Operand.Imm(offset), curMcBlock);
                    mcBinary.setNeedFix(McBinary.FixType.PARAM_STACK);
                    new McLdr(dst, addr, curMcBlock);
                    curMcFunc.addStackSize(4);
                    regStack ++;
                }
            } else {
                if(rRegIdx < rParamCnt) {
                    new McMove( dst, Operand.PhyReg.getPhyReg(rRegIdx), curMcBlock);
                    rRegIdx ++;
                } else {
                    int offset = -(regStack + 1) * 4;
                    Operand addr = new Operand.VirtualReg(false, curMcFunc);
                    McBinary mcBinary =new McBinary(McBinary.BinaryType.Add, addr, Operand.PhyReg.getPhyReg("sp"),
                            new Operand.Imm(offset), curMcBlock);
                    mcBinary.setNeedFix(McBinary.FixType.PARAM_STACK);
                    new McLdr(dst, addr, curMcBlock);
                    curMcFunc.addStackSize(4);
                    regStack ++;
                }
            }
        }

    }

    public void genBasicBlock(BasicBlock basicBlock){
        curMcBlock = blockMap.get(basicBlock);
        curMcBlock.setMcFunction(curMcFunc);
        curMcFunc.addMcBlock(curMcBlock);
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
                McBlock elseBlock = blockMap.get(((Branch) instr).getElseBlock());
                thenBlock.addPreMcBlock(curMcBlock);
                elseBlock.addPreMcBlock(curMcBlock);
                boolean exchanged = false;
                if(visitBBset.add(elseBlock)) {
                    nextBBList.push(elseBlock.getBasicBlock());
                }
                if(visitBBset.add(thenBlock)) {
                    nextBBList.push(thenBlock.getBasicBlock());
                    exchanged = true;
                }
//                System.out.println(instr);
                Operand opd = value2Imm.get(cond);
                if(opd != null) {
                    if(((Operand.Imm) opd).getIntNumber() == 1) {
                        new McJump(thenBlock, curMcBlock);
                    } else{
                        assert ((Operand.Imm) opd).getIntNumber() == 0;
                        new McJump(elseBlock, curMcBlock);
                    }
                    continue;
                }

                Cond mcCond = value2cond.get(cond);
                if(exchanged) {
                    McBlock tmp = thenBlock;
                    thenBlock = elseBlock;
                    elseBlock = tmp;
                    if(cond instanceof Fcmp) {
                        mcCond = getFcmpOppoCond(mcCond);
                    } else {
                        mcCond = getIcmpOppoCond(mcCond);
                    }
                }
                curMcBlock.addSuccMcBlock(thenBlock);
                curMcBlock.addSuccMcBlock(elseBlock);
                new McBranch( mcCond, thenBlock, curMcBlock);
                new McJump( elseBlock, curMcBlock);
            }
            else if(instr instanceof Jump) {
                BasicBlock block = ((Jump) instr).getTargetBlock();
                McBlock targetBlock = blockMap.get(block);
                curMcBlock.addSuccMcBlock(targetBlock);
                targetBlock.addPreMcBlock(curMcBlock);
                if(visitBBset.add(targetBlock)) {
                    nextBBList.push(block);
                }
                new McJump(targetBlock, curMcBlock);
            }
            else if(instr instanceof BitCast) {
                Operand opd = getOperand(instr.getUse(0));
                value2opd.put(instr, opd);
            }
            else if(instr instanceof Alloc) {
                Alloc alloc = (Alloc) instr;
                Type type = alloc.getType().getBasicType();
                // 其他的应该在mem2reg阶段已经被删除了
                assert type instanceof ArrayType;
                Operand addr = getOperand(alloc);
                Operand offset = getOperand(new Variable.ConstInt(curMcFunc.getStackSize()));
                curMcFunc.addStackSize(((ArrayType) type).getFattenSize() * 4);
                new McBinary(McBinary.BinaryType.Add, addr,
                        Operand.PhyReg.getPhyReg("sp"), offset, curMcBlock);
            }
            else if(instr instanceof Call) {
                genCall((Call) instr);
            }
            else if(instr instanceof Fptosi) {
                Operand src = getOperand(instr.getUse(0));
                Operand tmp = new Operand.VirtualReg(true, curMcFunc);
                Operand dst = getOperand(instr);
                new McVcvt(McVcvt.VcvtType.f2i, tmp, src, curMcBlock);
                new McMove(dst, tmp, curMcBlock);
            }
            else if(instr instanceof GetElementPtr) {
                Value pointer = ((GetElementPtr) instr).getPointer();
                ArrayList<Value> idxList = ((GetElementPtr) instr).getIdxList();
                Type curBasicType = pointer.getType().getBasicType();
                Operand addrOpd = getOperand(pointer);
                if(addrOpd instanceof Operand.Global) {
                    Operand temp = new Operand.VirtualReg(false, curMcFunc);
                    new McMove(temp, addrOpd, curMcBlock);
                    addrOpd = temp;
                }
                Operand dst = getOperand(instr);
                Operand tmpAddr = null;
                int allOff = 0;
                for(int i = 0; i < idxList.size(); i++) {
                    Value idx = idxList.get(i);
                    int offset = 4;
                    if(curBasicType instanceof ArrayType) {
                        offset = offset * ((ArrayType) curBasicType).getFattenSize();
                        curBasicType = curBasicType.getBasicType();
                    }
                    if(idx instanceof Variable.ConstInt) {
                        int sum = ((Variable.ConstInt) idx).getIntVal() * offset;
//                        System.out.println(sum);
                        allOff += sum;
                    } else {
                        Operand tmp = getOperand(idx);
                        Operand mulAns = new Operand.VirtualReg(false, curMcFunc);
                        Operand mulOff = getOperand(new Variable.ConstInt(offset));
                        if(mulOff instanceof Operand.Imm) {
                            Operand temp = new Operand.VirtualReg(mulOff.isFloat(), curMcFunc);
                            new McMove(temp, mulOff, curMcBlock);
                            mulOff = temp;
                        }
                        new McBinary(McBinary.BinaryType.Mul, mulAns, tmp,
                                mulOff, curMcBlock);
                        if(tmpAddr == null) {
                            tmpAddr = new Operand.VirtualReg(false, curMcFunc);
                            new McBinary(McBinary.BinaryType.Add, tmpAddr, addrOpd, mulAns, curMcBlock);
                        } else {
                            new McBinary(McBinary.BinaryType.Add, tmpAddr, tmpAddr, mulAns, curMcBlock);
                        }
                    }
                    if(i == idxList.size() - 1) {
                        if(allOff == 0) {
                            if(tmpAddr == null)
                                new McMove(dst, addrOpd, curMcBlock);
                            else
                                new McMove(dst, tmpAddr, curMcBlock);
                        } else {
                            if(tmpAddr == null)
                                new McBinary(McBinary.BinaryType.Add, dst, addrOpd,
                                        getOperand(new Variable.ConstInt(allOff)), curMcBlock);
                            else
                                new McBinary(McBinary.BinaryType.Add, dst, tmpAddr,
                                        getOperand(new Variable.ConstInt(allOff)), curMcBlock);
                        }
                    }
                }
            }
            else if(instr instanceof Store){
                Value data = ((Store) instr).getValue();
                Value addr = ((Store) instr).getAddress();
                Operand dtOpd = getOperand(data);
                Operand addrOpd = getOperand(addr);
                if(addrOpd instanceof Operand.Global) {
                    Operand temp = new Operand.VirtualReg(false, curMcFunc);
                    new McMove(temp, addrOpd, curMcBlock);
                    addrOpd = temp;
                }
                if(dtOpd instanceof Operand.Imm) {
                    Operand temp = new Operand.VirtualReg(dtOpd.isFloat(), curMcFunc);
                    new McMove(temp, dtOpd, curMcBlock);
                    dtOpd = temp;
                }
//                System.out.println(dtOpd);
                new McStore(dtOpd, addrOpd, curMcBlock);
            }
            else if(instr instanceof Load) {
                Value addr = ((Load) instr).getPointer();
                Operand dtOpd = getOperand(instr);
                Operand addrOpd = getOperand(addr);
                if(addrOpd instanceof Operand.Global) {
                    Operand temp = new Operand.VirtualReg(false, curMcFunc);
                    new McMove(temp, addrOpd, curMcBlock);
                    addrOpd = temp;
                }
                new McLdr(dtOpd, addrOpd, curMcBlock);
            }
            else if(instr instanceof Return) {
                Value ret = ((Return) instr).getReturnValue();
                if(ret != null) {
                    Operand retOpd;
                    if(ret.getType() instanceof Int32Type) {
                        retOpd = getOperand(ret);
                        new McMove(Operand.PhyReg.getPhyReg("r0"), retOpd, curMcBlock);
                    }else{
                        assert ret.getType() instanceof ir.type.FloatType;
                        retOpd = getOperand(ret);
                        new McMove(Operand.FPhyReg.getFPhyReg("s0"), retOpd, curMcBlock);
                    }
                    McBinary mcBinary = new McBinary(McBinary.BinaryType.Add, Operand.PhyReg.getPhyReg("sp"),
                            Operand.PhyReg.getPhyReg("sp"), new Operand.Imm(0), curMcBlock);
                    mcBinary.setNeedFix(McBinary.FixType.VAR_STACK);
                    new McReturn(retOpd, curMcBlock);
                } else {
                    McBinary mcBinary = new McBinary(McBinary.BinaryType.Add, Operand.PhyReg.getPhyReg("sp"),
                            Operand.PhyReg.getPhyReg("sp"), new Operand.Imm(0), curMcBlock);
                    mcBinary.setNeedFix(McBinary.FixType.VAR_STACK);
                    new McReturn(curMcBlock);
                }
            }
            else if(instr instanceof Sitofp) {
                Operand src = getOperand(instr.getUse(0));
                if(src instanceof Operand.Imm) {
                    Operand temp = new Operand.VirtualReg(src.isFloat(), curMcFunc);
                    new McMove(temp, src, curMcBlock);
                    src =temp;
                }
                Operand tmp = new Operand.VirtualReg(true, curMcFunc);
//                System.out.println(instr.type + "!!!!!!!!!!!!!");
                Operand dst = getOperand(instr);
//                System.out.println(dst.isFloat());
                new McMove(tmp, src, curMcBlock);
                new McVcvt(McVcvt.VcvtType.i2f, dst, tmp, curMcBlock);
            }
            else if(instr instanceof Unary) {
                OpTree.Operator op = ((Unary) instr).getOp();
                if(op.equals(OpTree.Operator.Neg)) {
                    Operand src;


                    if(instr.type instanceof ir.type.FloatType) {
                        src = getOperand(((Unary) instr).getVal());
                        Operand dst = getOperand(instr);
                        new McNeg( dst, src, curMcBlock);
                    } else {
                        if(((Unary) instr).getVal() instanceof Variable.ConstInt) {
                            Unary unary = (Unary) instr;
                            int ans = 0 - ((Variable.ConstInt)unary.getVal()).getIntVal();
                            value2opd.put(instr, getOperand(new Variable.ConstInt(ans)));
                        } else {
                            src = getOperand(((Unary) instr).getVal());
                            Operand dst = getOperand(instr);
                            new McBinary(McBinary.BinaryType.Rsb, dst, src, new Operand.Imm(0), curMcBlock);
                        }

                    }
                } else {
                    System.err.println("unary中的not直接生成了fcmp和icmp");
                }
            }
            else if(instr instanceof Zext) {
                Operand dst = getOperand(instr.getUse(0));
                value2opd.put(instr, dst);
            }
            else if(instr instanceof Move) {
                Operand dst = getOperand(((Move) instr).getTo());
                Operand src = getOperand(((Move) instr).getFrom());
                new McMove(dst, src, curMcBlock);
            }
            else {
                System.err.println("存在ir类型" + instr.getClass() + "未被解析为lir");
            }
        }
    }

    public void genCall(Call call) {
        ArrayList<Value> params = call.getParams();
        Function callFunc = call.getFunction();
        int sRegIdx = 0;    //  当前用到的浮点数寄存器下标
        int rRegIdx = 0;    //  当前用到的整数寄存器下标
        int regStack = 0;   //  溢出到栈上的寄存器数量
        for(Value param: params) {
            if(param.getType() instanceof ir.type.FloatType) {
                Operand src = getOperand(param);
                //  可以存放入寄存器中
                if(sRegIdx < sParamCnt) {
                    new McMove( Operand.FPhyReg.getFPhyReg("s" + sRegIdx), src, curMcBlock);
                    sRegIdx ++;
                }
                else {
                    int offset = - (regStack + 1) * 4;
                    if(canImmOffset(offset)){
                        new McStore(src, Operand.PhyReg.getPhyReg("sp"), new Operand.Imm(offset), curMcBlock);
                    } else {
                        Operand addr = new Operand.VirtualReg(false, curMcFunc);
                        Operand imm = getOperand( new Variable.ConstInt(-offset));
                        new McBinary(McBinary.BinaryType.Sub, addr, Operand.PhyReg.getPhyReg("sp"), imm, curMcBlock);
                        new McStore(src, addr, curMcBlock);
                    }
                    regStack ++;
                }
            }
            else {
                Operand src = getOperand(param);
                if(rRegIdx < rParamCnt) {
                    new McMove(Operand.PhyReg.getPhyReg("r" + rRegIdx), src, curMcBlock);
                    rRegIdx ++;
                }
                else {
                    int offset = - (regStack + 1) * 4;
                    if(src instanceof Operand.Imm) {
                        Operand temp = new Operand.VirtualReg(src.isFloat(), curMcFunc);
                        new McMove(temp, src, curMcBlock);
                        src =temp;
                    }
                    if(canImmOffset(offset)){
                        new McStore(src, Operand.PhyReg.getPhyReg("sp"), new Operand.Imm(offset), curMcBlock);
                    } else {
                        Operand addr = new Operand.VirtualReg(false, curMcFunc);
                        Operand imm = getOperand( new Variable.ConstInt(-offset));
                        new McBinary(McBinary.BinaryType.Sub, addr, Operand.PhyReg.getPhyReg("sp"), imm, curMcBlock);
                        new McStore(src, addr, curMcBlock);
                    }
                    regStack ++;
                }
            }
        }

        McFunction callMcFunc = funcMap.get(callFunc);
        McCall mcCall = new McCall(callMcFunc, curMcBlock);
        mcCall.setsRegIdx(sRegIdx);
        mcCall.setrRegIdx(rRegIdx);

        if(call.type instanceof Int32Type) {
            Operand dst = getOperand(call);
            new McMove(dst, Operand.PhyReg.getPhyReg("r0"), curMcBlock);
        } else if(call.type instanceof ir.type.FloatType) {
            Operand dst = getOperand(call);
            new McMove(dst, Operand.FPhyReg.getFPhyReg("s0"), curMcBlock);
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
                if(icmp.getNext() instanceof Branch
                        && icmp.getUser(0).equals(icmp.getNext())) {
                    value2Imm.put(instr, imm);
                } else {
                    new McMove(dstVr, imm, curMcBlock);
                }

            }
            else {
//                System.out.println("---------------");
//                System.out.println(instr);
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
                if(icmp.getNext() instanceof Branch && icmp.getUser(0).equals(icmp.getNext())) {
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
                Operand.Imm imm;
                if(ans){
                    imm = new Operand.Imm(1);
                }else {
                    imm = new Operand.Imm(0);
                }
                if(fcmp.getNext() instanceof Branch && fcmp.getUsedSize() == 1
                        && fcmp.getUser(0).equals(fcmp.getNext())) {
                    value2Imm.put(instr, imm);
                } else {
                    new McMove(dstVr, imm, curMcBlock);
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
                    if(left instanceof Variable.ConstInt && right instanceof Variable.ConstInt){
                        int valLeft = ((Variable.ConstInt)left).getIntVal();
                        new McMove(dstVr, getOperand(new Variable.ConstInt(valLeft % imm)),curMcBlock);
                    } else {
                        Operand lopd = getOperand(left);
                        Operand ropd = getOperand(right);
                        if(ropd instanceof Operand.Imm) {
                            Operand temp = new Operand.VirtualReg(ropd.isFloat(), curMcFunc);
                            new McMove(temp, ropd, curMcBlock);
                            ropd = temp;
                        }
                        Operand dst1 = new Operand.VirtualReg(left instanceof Variable.ConstFloat, curMcFunc);
                        new McBinary(McBinary.BinaryType.Div, dst1, lopd, ropd, curMcBlock);
                        new McBinary(McBinary.BinaryType.Mul, dst1, dst1, ropd, curMcBlock);
                        new McBinary(McBinary.BinaryType.Sub, dstVr, lopd, dst1, curMcBlock);
                    }
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
//                        System.out.println(instr);
                        // 2的多少次方
                        int sh = 31 - Integer.numberOfLeadingZeros(abs);
                        if(sh == 0) {
                            new McMove(dstVr, src, curMcBlock);
                        } else {
                            MCShift mcShift = new MCShift(dstVr, src, getOperand(new Variable.ConstInt(sh)),MCShift.ShiftType.lsl,curMcBlock);
                        }
                        if(imm < 0) {
                            new McBinary(McBinary.BinaryType.Rsb, dstVr, dstVr, new Operand.Imm(0),curMcBlock);
                        }
                    }
                    else {
                        Operand src2 = getOperand(new Variable.ConstInt(imm));
                        if(src2 instanceof Operand.Imm) {
                            Operand temp = new Operand.VirtualReg(false, curMcFunc);
                            new McMove(temp, src2, curMcBlock);
                            src2 = temp;
                        }
                        new McBinary(McBinary.BinaryType.Mul, dstVr, src, src2, curMcBlock);
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
                        Operand immOpd = getOperand(new Variable.ConstInt(imm));
                        if(immOpd instanceof Operand.Imm) {
                            Operand temp = new Operand.VirtualReg(immOpd.isFloat(), curMcFunc);
                            new McMove(temp, immOpd, curMcBlock);
                            immOpd = temp;
                        }
                        new McBinary(McBinary.BinaryType.Div, dstVr, lopd, immOpd, curMcBlock);
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
            Manager.addGlobalOpds((Operand.Global) opd);
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

    public static boolean canImmOffset(int imm) {
        return imm <= 1020 && imm >= -1020;
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
