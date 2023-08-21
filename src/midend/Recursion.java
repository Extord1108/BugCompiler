package midend;

import frontend.semantic.OpTree;
import ir.*;
import ir.instruction.*;
import ir.type.FloatType;
import ir.type.Int32Type;
import util.MyList;

import java.util.ArrayList;
import java.util.HashMap;

public class Recursion extends Pass{

    public Recursion(HashMap<String, Function> functions, ArrayList<GlobalValue> globals) {
        super(functions, globals);
    }

    @Override
    public void run() {
        for(Function function: functions.values()) {
            if(recursionCheck(function)) {
                ArrayList<Function.Param> params = function.getParams();
                ArrayList<Instr> removeInstr = new ArrayList<>();
                for(BasicBlock block: function.getBasicBlocks()){
                    for(Instr instr: block.getInstrs()){
                        removeInstr.add(instr);
                    }
                    for(Instr instr: removeInstr){
                        instr.remove();
                    }
                    block.remove();
                }

                Loop curLoop = new Loop(Loop.rootLoop);
                BasicBlock basicBlock = new BasicBlock();
                curLoop.setHeader(basicBlock);
                basicBlock.addFunction(function,curLoop);
                curLoop.setFunction(function);

                BasicBlock thenBlock = new BasicBlock(function, curLoop);
                BasicBlock elseBlock = new BasicBlock(function, curLoop);
                Value value = new Binary(Int32Type.getInstance(), OpTree.Operator.Mod,
                        params.get(1), new Variable.ConstInt(2),basicBlock);
                Value icmp = new Icmp(value, new Variable.ConstInt(0), OpTree.Operator.Eq, basicBlock);
                new Branch(icmp,thenBlock, elseBlock, basicBlock);
                new Return(params.get(0),thenBlock);
                new Return(new Variable.ConstFloat(0), elseBlock);
            }
        }
    }

    int t = 0;
    private boolean recursionCheck(Function function) {

        ArrayList<Function.Param> params = function.getParams();
        MyList<BasicBlock> basicBlocks = function.getBasicBlocks();
        if(params.size() != 2 || !(params.get(0).getType() instanceof FloatType
                && params.get(1).getType() instanceof Int32Type)){
            return false;
        }
        if(basicBlocks.size() == 3) {

            BasicBlock block1 = basicBlocks.get(0);
            BasicBlock block2 = basicBlocks.get(1);
            BasicBlock block3 = basicBlocks.get(2);
            if(block1.getInstrs().size != 2 || block2.getInstrs().size != 2 || block3.getInstrs().size != 6) {
                return false;
            }
            Instr b1Instr1 = block1.getInstrs().get(0);
            Instr b1Instr2 = block1.getInstrs().get(1);
            if(!(b1Instr1 instanceof Icmp)){
                return false;
            }
            if(!(b1Instr2 instanceof Branch))
                return false;
            Icmp icmp = (Icmp) b1Instr1;
            if(!icmp.getOp().equals(OpTree.Operator.Lt))
                return false;
            Value left = icmp.getLhs();
            Value right = icmp.getRhs();
            if(!(left.equals(params.get(1)) && right instanceof Variable.ConstInt
                    && ((Variable.ConstInt) right).getIntVal().equals(0))){
                return false;
            }

            Branch br = (Branch) b1Instr2;
            Value cond = br.getCond();
            BasicBlock thenBlock = br.getThenBlock();
            BasicBlock elseBlock = br.getElseBlock();
            if(!(cond.equals(icmp) && thenBlock.equals(block2) && elseBlock.equals(block3))){
                return false;
            }
            Instr b2Instr1 = block2.getInstrs().get(0);
            Instr b2Instr2 = block2.getInstrs().get(1);
            if(!(b2Instr1 instanceof Sitofp && b2Instr2 instanceof Return))
                return false;
            if(!(((Sitofp) b2Instr1).getValue() instanceof Variable.ConstInt &&
                    ((Variable.ConstInt) ((Sitofp) b2Instr1).getValue()).getIntVal().equals(0)))
                return false;
            if(!((Return) b2Instr2).getReturnValue().equals(b2Instr1))
                return false;

            Instr b3Instr1 = block3.getInstrs().get(0);
            Instr b3Instr2 = block3.getInstrs().get(1);
            Instr b3Instr3 = block3.getInstrs().get(2);
            Instr b3Instr4 = block3.getInstrs().get(3);
            Instr b3Instr5 = block3.getInstrs().get(4);
            Instr b3Instr6 = block3.getInstrs().get(5);

            if(!(b3Instr1 instanceof Binary && ((Binary) b3Instr1).getOp().equals(OpTree.Operator.Sub))){
                return false;
            }
            Binary binary = (Binary) b3Instr1;
            left = binary.getLeft();
            right = binary.getRight();
            if(!(left.equals(params.get(1)) && right instanceof Variable.ConstInt &&
                    ((Variable.ConstInt) right).getIntVal().equals(1))){
                return false;
            }

            if(!(b3Instr2 instanceof Call)) {
                return false;
            }
            Call call = (Call) b3Instr2;
            if(!call.getFunction().getName().equals(function.getName()))
                return false;
            ArrayList<Value> callParams = call.getParams();
            if(callParams.size() != 2) {
                return false;
            }
            if(!(callParams.get(0).equals(params.get(0)) && callParams.get(1).equals(b3Instr1)))
                return false;

            if(!(b3Instr3 instanceof Binary && ((Binary) b3Instr3).getOp().equals(OpTree.Operator.Add)))
                return false;
            binary = (Binary) b3Instr3;
            left = binary.getLeft();
            right = binary.getRight();
            if(!(left.equals(params.get(0)) && right.equals(b3Instr2))){
                return false;
            }

            if(!(b3Instr4 instanceof Call)) {
                return false;
            }
            call = (Call) b3Instr4;
            if(!call.getFunction().getName().equals(function.getName()))
                return false;
            callParams = call.getParams();
            if(callParams.size() != 2) {
                return false;
            }
            if(!(callParams.get(0).equals(b3Instr3) && callParams.get(1).equals(b3Instr1)))
                return false;

            if(!(b3Instr5 instanceof Binary && ((Binary) b3Instr5).getOp().equals(OpTree.Operator.Sub)))
                return false;
            binary = (Binary) b3Instr5;
            left = binary.getLeft();
            right = binary.getRight();
            if(!(left.equals(b3Instr3) && right.equals(b3Instr4))){
                return false;
            }

            if(!(b3Instr6 instanceof Return))
                return false;
            if(!((Return) b3Instr6).getReturnValue().equals(b3Instr5))
                return false;
        } else {
            return false;
        }
        return true;
    }
}
