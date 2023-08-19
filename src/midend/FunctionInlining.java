package midend;

import ir.*;
import ir.instruction.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

public class FunctionInlining extends Pass{

    private final int MAX_INSTR = 32;
    public FunctionInlining(HashMap<String, Function> functions, ArrayList<GlobalValue> globals) {
        super(functions, globals);
    }

    @Override
    public void run() {
        for(Function function : this.functions.values()){
            if(canInline(function)){
                inline(function);
                cleanClone(function);
            }
        }
        ArrayList<Function> functions = new ArrayList<>(this.functions.values());
        for(Function function : functions){
            if(canInline(function) && function.getUsedInfo().size() == 0){
                this.functions.remove(function.getName());
            }
        }
    }

    private void inline(Function function){
        for(Used used:function.getUsedInfo()){
            Instr user = used.getUser();
            assert user instanceof Call;
            Call call = (Call) user;
            Function callerFunction = call.getBasicBlock().getFunction();
            BasicBlock callerPredBB = call.getBasicBlock();
            BasicBlock callerSuccBB = new BasicBlock(callerFunction, callerPredBB.getLoop());
            callerSuccBB.remove();
            callerFunction.getBasicBlocks().insertAfter(callerPredBB, callerSuccBB);
            //以call为界，将callerPredBB分为两个BB
            for(int i = 0;i < callerPredBB.getInstrs().size();i++){
                if(callerPredBB.getInstrs().get(i) == call){
                    ArrayList<Instr> instrsOfNextBB = new ArrayList<>();
                    for(int j = i + 1;j < callerPredBB.getInstrs().size();j++){
                        instrsOfNextBB.add(callerPredBB.getInstrs().get(j));
                    }
                    for(Instr instr : instrsOfNextBB){
                        callerPredBB.getInstrs().remove(instr);
                        callerSuccBB.addInstr(instr);
                        instr.setBasicBlock(callerSuccBB);
                    }
                    break;
                }
            }
            //复制函数的形参
            for(int i=0;i<function.getParams().size();i++){
                function.getParams().get(i).clone();
            }
            //将function的BB插入callerPredBB和callerSuccBB之间
            BasicBlock lastBB = callerPredBB;
            BasicBlock bbClone = null;

            //先按顺序插入block
            for(int i = 0;i < function.getBasicBlocks().size();i++){
                BasicBlock bb = function.getBasicBlocks().get(i);
                Loop cloneLoop = null;
                if(bb.getLoop().getParent().equals(Loop.rootLoop))
                {
                    if(bb.getLoop().getCloneLoop()==null)
                        cloneLoop = bb.getLoop().clone(callerPredBB.getLoop().getParent());
                    else
                        cloneLoop = bb.getLoop().getCloneLoop();
                }
                else
                {
                    if(bb.getLoop().getCloneLoop()==null)
                        cloneLoop = bb.getLoop().clone(bb.getLoop().getParent().getCloneLoop());
                    else
                        cloneLoop = bb.getLoop().getCloneLoop();
                }
                bbClone = bb.clone(callerFunction, cloneLoop);
                bbClone.remove();
                callerFunction.getBasicBlocks().insertAfter(lastBB, bbClone);
                lastBB = bbClone;
            }

            //按逆后序向block中插入instr
            ReversePostOrder reversePostOrder = new ReversePostOrder(function);
            ArrayList<BasicBlock> reversePostOrderBB = reversePostOrder.get();
            lastBB = callerPredBB;
            for(BasicBlock bb:reversePostOrderBB){
                bbClone = bb.getClone();
                for(Instr instr:bb.getInstrs()){
                    instr.clone(bbClone);
                }
                if(lastBB.equals(callerPredBB)){
                    new Jump(bbClone, lastBB);
                }
                lastBB = bbClone;
            }

            //对phi更新use
            for(BasicBlock bb:reversePostOrderBB){
                for(Instr instr:bb.getInstrs()){
                    if(instr instanceof Phi){
                        Phi phi =  (Phi)(instr.getClone());
                        int useSize = phi.getUses().size();
                        for(int i=0;i<useSize;i++){
                            phi.modifyUse(i,phi.getUse(i).getClone());
                        }
                    }
                }
            }

            //将function的形参替换为实参
            for(int i = 0;i < function.getParams().size();i++){
                function.getParams().get(i).getClone().repalceUseofMeto(call.getParams().get(i));
            }

            //将对call的使用替换为对return value的使用
            Return retOfFunction = null;
            for(int i = 0;i < function.getBasicBlocks().size();i++){
                BasicBlock bb = function.getBasicBlocks().get(i);
                if((bb.getInstrs().size()>0) && (bb.getInstrs().getLast() instanceof Return)){
                    retOfFunction = (Return) bb.getInstrs().getLast().getClone();
                    break;
                }
            }
            if(retOfFunction.getReturnValue()!=null){
                //如果有返回值，将返回值的clone作为call的返回值
                call.repalceUseofMeto(retOfFunction.getReturnValue());
            }
            call.remove();
            retOfFunction.remove();
            //将return的bb的jump指向callerSuccBB
            new Jump(callerSuccBB, retOfFunction.getBasicBlock());

            //维护cfg图
            new ControlFlowGraph(callerFunction).run();
        }
    }

    private boolean canInline(Function function){
        if(function.isExternal() || function.getName().equals("main")) return false;
        if(function.getUsedInfo().size() == 0) return true;
        int instrNum = 0;//指令不超过32条
        int retNum = 0;//只有一个return
        for(BasicBlock bb : function.getBasicBlocks()){
            instrNum += bb.getInstrs().size();
            if(instrNum > MAX_INSTR) return false;
            for(Instr instr:bb.getInstrs()){
                if(instr instanceof Call && ((Call)instr).getFunction().equals(function)) return false;
                if(instr instanceof Return){
                    retNum++;
                    if(retNum > 1) return false;
                }
            }
        }
        return true;
    }

    private void cleanClone(Function function){
        for(BasicBlock bb:function.getBasicBlocks()){
            bb.cleanClone();
            for(Instr instr:bb.getInstrs()){
                instr.cleanClone();
            }
        }
        for(Function.Param param:function.getParams()){
            param.cleanClone();
        }
    }
}
