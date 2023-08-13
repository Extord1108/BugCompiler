package midend;

import frontend.semantic.InitVal;
import frontend.semantic.OpTree;
import ir.*;
import ir.instruction.Binary;
import ir.instruction.Instr;
import ir.type.Int32Type;
import util.MyList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class InstrComb extends Pass{

    public InstrComb(HashMap<String, Function> functions, ArrayList<GlobalValue> globals) {
        super(functions, globals);
    }

    @Override
    public void run()
    {
        for(Function function : this.functions.values())
        {
            if(function.isExternal()) continue;
            runForFunc(function);
        }
    }

    private void runForFunc(Function function)
    {
        LinkedList<Instr> queue = new LinkedList<>();
        for(BasicBlock bb : function.getBasicBlocks())
        {
            for(Instr instr : bb.getInstrs())
            {
                if(instr instanceof Binary && ((Binary) instr).getOp() == OpTree.Operator.Add)
                {
                    if(hasConst(instr)){
                        queue.add(instr);
                        break;
                    }
                }
            }
        }

        while(!queue.isEmpty()){
            Binary instr = (Binary) queue.poll();
            for(Used used:instr.getUsedInfo()){
                Instr user = used.getUser();
                if(user instanceof Binary){
                    Binary binary = (Binary) user;
                    if(binary.getOp() == OpTree.Operator.Add || binary.getOp() == OpTree.Operator.Sub){
                        if(judge(binary,instr)){
                            if(binary.getLeft() instanceof Variable.ConstInt && instr.getLeft() instanceof  Variable.ConstInt
                            && !(binary.getLeft() instanceof Function.Param) && !(instr.getLeft() instanceof Function.Param)){
                                Integer ans = calc(((Variable.ConstInt) instr.getLeft()).getIntVal(),(((Variable.ConstInt) binary.getLeft()).getIntVal()),binary.getOp());
                                combOther(queue,instr,binary,ans);
                            }
                            else if(binary.getLeft() instanceof Variable.ConstInt && instr.getRight() instanceof Variable.ConstInt
                            && !(binary.getLeft() instanceof Function.Param) && !(instr.getRight() instanceof Function.Param)){
                                Integer ans = calc(((Variable.ConstInt) instr.getRight()).getIntVal(),(((Variable.ConstInt) binary.getLeft()).getIntVal()),binary.getOp());
                                comb(queue,instr,binary,ans);
                            }
                            else if(binary.getRight() instanceof Variable.ConstInt && instr.getLeft() instanceof Variable.ConstInt
                            && !(binary.getRight() instanceof Function.Param) && !(instr.getLeft() instanceof Function.Param)){
                                Integer ans = calc(((Variable.ConstInt) instr.getLeft()).getIntVal(),(((Variable.ConstInt) binary.getRight()).getIntVal()),binary.getOp());
                                combOther(queue,instr,binary,ans);
                            }
                            else if(binary.getRight() instanceof Variable.ConstInt && instr.getRight() instanceof Variable.ConstInt
                            && !(binary.getRight() instanceof Function.Param) && !(instr.getRight() instanceof Function.Param)){
                                Integer ans = calc(((Variable.ConstInt) instr.getRight()).getIntVal(),(((Variable.ConstInt) binary.getRight()).getIntVal()),binary.getOp());
                                comb(queue,instr,binary,ans);
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean hasConst(Instr instr){
        Binary binary = (Binary) instr;
        return (binary.getLeft() instanceof Variable.ConstInt && !(binary.getRight() instanceof Variable.ConstInt) &&
                (binary.getLeft().getType().equals(Int32Type.getInstance())) && !(binary.getLeft() instanceof  Function.Param))
                || (binary.getRight() instanceof Variable.ConstInt && !(binary.getLeft() instanceof Variable.ConstInt) &&
                (binary.getRight().getType().equals(Int32Type.getInstance())) && !(binary.getRight() instanceof  Function.Param));
    }

    private boolean judge(Binary binary,Instr instr){
        return (binary.getLeft() instanceof Variable.ConstInt && (binary.getRight().equals(instr)))
                || (binary.getRight() instanceof Variable.ConstInt && (binary.getLeft().equals(instr)));
    }

    private void combOther(LinkedList<Instr> queue,Binary binary,Instr user,Integer ans){
        Variable.ConstInt constInt = new Variable.ConstInt(ans);
        Binary newBinary = new Binary(binary.getType(),binary.getOp(),binary.getRight(),constInt,user);
        //binary.getRight().addUsed(new Used(newBinary,binary));
        user.repalceUseofMeto(newBinary);
        user.remove();
        queue.add(newBinary);
    }

    private void comb(LinkedList<Instr> queue,Binary binary,Instr user,Integer ans){
        Variable.ConstInt constInt = new Variable.ConstInt(ans);
        Binary newBinary = new Binary(binary.getType(),binary.getOp(),binary.getLeft(),constInt,user);
        //binary.getLeft().addUsed(new Used(newBinary,binary));
        user.repalceUseofMeto(newBinary);
        user.remove();
        queue.add(newBinary);
    }

    private Integer calc(Integer a,Integer b,OpTree.Operator op)
    {
        switch (op)
        {
            case Add:
                return a + b;
            case Sub:
                return a - b;
            case Mul:
                return a * b;
            case Div:
                return a / b;
            case Mod:
                return a % b;
            case And:
                return a & b;
            case Or:
                return a | b;
            default:
                return null;
        }
    }
}
