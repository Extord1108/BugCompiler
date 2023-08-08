package midend;

import ir.*;
import ir.instruction.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class GCM extends Pass {

    private HashSet<Instr> visitedInstrs = new HashSet<>();
    private BasicBlock root;
    private Function curFunc;
    public GCM(HashMap<String, Function> functions, ArrayList<GlobalValue> globals) {
        super(functions, globals);
    }
    @Override
    public void run(){
        //scheduleEarly
        for(Function function : this.functions.values())
        {
            if(function.isExternal()) continue;
            visitedInstrs.clear();
            curFunc = function;
            scheduleEarly(function);
        }
        //scheduleLate
        for(Function function : this.functions.values())
        {
            if(function.isExternal()) continue;
            visitedInstrs.clear();
            curFunc = function;
            scheduleLate(function);
        }
        //move to fit position
        for(Function function : this.functions.values())
        {
            if(function.isExternal()) continue;
            visitedInstrs.clear();
            curFunc = function;
            moveInstr(function);
        }
    }

    private void scheduleEarly(Function function){
        root = function.getBasicBlocks().get(0);
        for(BasicBlock bb:function.getBasicBlocks())
        {
            for(Instr instr: bb.getInstrs()){
                if(isInstrPinned(instr)){
                    visitedInstrs.add(instr);
                    for(Value value : instr.getUses()){
                        if(value instanceof Instr)
                            scheduleEarlyForOperands((Instr) value);
                    }
                }
            }
        }
    }

    private void scheduleEarlyForOperands(Instr instr){
        if(visitedInstrs.contains(instr) || isInstrPinned(instr)) return;
        visitedInstrs.add(instr);
        instr.setEarliestBasicBlock(root);//将入口块设置为最早块
        for(Value value : instr.getUses()){
            if(value instanceof Instr)
            {
                Instr useInstr = (Instr) value;
                scheduleEarlyForOperands(useInstr);
                BasicBlock opBB = useInstr.getBasicBlock();
                if(opBB.getDomTreeDepth() < instr.getEarliestBasicBlock().getDomTreeDepth())
                    instr.setEarliestBasicBlock(opBB);
            }
        }
    }

    private void scheduleLate(Function function){
        root = function.getBasicBlocks().get(0);
        for(BasicBlock bb:function.getBasicBlocks())
        {
            for(Instr instr: bb.getInstrs()){
                instr.setLatestBasicBlock(instr.getBasicBlock());
                if(isInstrPinned(instr)){
                    visitedInstrs.add(instr);
                    for(Used used: instr.getUsedInfo()){
                        scheduleLateForOperands(used.getUser());
                    }
                }
            }
        }
    }

    private void scheduleLateForOperands(Instr instr) {
        if(visitedInstrs.contains(instr) || isInstrPinned(instr)) return;
        visitedInstrs.add(instr);
        BasicBlock lca = null;
        for(Used used: instr.getUsedInfo()){
            Instr user = used.getUser();
            scheduleLateForOperands(user);
            BasicBlock bb = user.getBasicBlock();
            if(user instanceof Phi){
                Phi phi = (Phi) user;
                for(int i = 0; i < phi.getUses().size(); i++){
                    if(phi.getUses().get(i).equals(instr)){
                        bb = bb.getPredecessors().get(i);
                        break;
                    }
                }
            }
            lca = findLCA(lca, bb);
        }
        BasicBlock best = lca;
        BasicBlock cur = lca;
        while(cur.getDomTreeDepth() != instr.getEarliestBasicBlock().getDomTreeDepth()){
            if(cur.getLoopDepth()< best.getLoopDepth()){
                best = cur;
            }
            cur = curFunc.getDomTree().get(cur);
        }
        if(cur.getLoopDepth()< best.getLoopDepth())
            best = cur;
        instr.setLatestBasicBlock(best);
    }

    private BasicBlock findLCA(BasicBlock a,BasicBlock b){
        if(a == null) return b;
        if(b == null) return a;
        if(a == b) return a;
        while(a.getDomTreeDepth() < b.getDomTreeDepth()){
            a = curFunc.getDomTree().get(a);
        }
        while(b.getDomTreeDepth() < a.getDomTreeDepth()){
            b = curFunc.getDomTree().get(b);
        }
        while(a != b){
            a = curFunc.getDomTree().get(a);
            b = curFunc.getDomTree().get(b);
        }
        return a;
    }

    private boolean isInstrPinned(Instr instr){
        return instr instanceof Phi || instr instanceof Branch ||
                instr instanceof Return || instr instanceof Jump ||
                instr instanceof Call || instr instanceof Store ||
                instr instanceof Load;
    }

    private void moveInstr(Function function){
        for(BasicBlock bb:function.getBasicBlocks())
        {
            ArrayList<Instr> instrs = new ArrayList<>();
            for(Instr instr: bb.getInstrs()){
                instrs.add(instr);
            }
            for(Instr instr: instrs){
                if(instr.getBasicBlock().equals(instr.getLatestBasicBlock())) continue;
                BasicBlock best = instr.getLatestBasicBlock();
                instr.remove();
                //best.addInstr(instr);
                instr.setBasicBlock(best);

                ArrayList<Instr> instrsOfBest = new ArrayList<>();
                for(Instr instrofBest: best.getInstrs()){
                    instrsOfBest.add(instrofBest);
                }
                int pos = instrsOfBest.size()-1;
                for(Used used: instr.getUsedInfo()){
                    Instr user = used.getUser();
                    int temp = instrsOfBest.indexOf(user);
                    if(temp!=-1){
                        pos = Math.min(pos, temp);
                    }
                }
                best.getInstrs().insertBefore(instrsOfBest.get(pos), instr);
            }

        }
    }
}
