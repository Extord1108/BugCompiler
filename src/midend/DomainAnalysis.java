package midend;

import ir.*;
import ir.instruction.Branch;
import ir.instruction.Instr;
import ir.instruction.Jump;
import util.MyList;
import midend.ReversePostOrder;

import java.util.*;

public class DomainAnalysis extends Pass {
    public DomainAnalysis(HashMap<String, Function> functions, ArrayList<GlobalValue> globals) {
        super(functions, globals);
    }
    public void run(){
        for(Function function : this.functions.values()) {
            if(function.isExternal()) {
                continue;
            }
            function.cleanDomInfo();
            new ControlFlowGraph(function).run();
            new RemoveDeadBB(function).run();
            new RemoveSingleBB(function).run();
            new DominatorTree(function).run();
            new DominanceFrontier(function).run();
            new LoopAnalysis(function).run();
        }
    }

    //删除仅有一个前驱且该前驱仅有一个后继，将基本块与且前驱合并
    private class RemoveSingleBB{
        private Function function;
        private RemoveSingleBB(Function function) {
            this.function = function;
        }
        public void run() {
            //后序遍历
            ArrayList<BasicBlock> postOrderBB = new ReversePostOrder(function).get();
            Collections.reverse(postOrderBB);
            for (int i = 0; i < postOrderBB.size(); i++) {
                BasicBlock bb = postOrderBB.get(i);
                if (bb.getPredecessors().size() == 1) {
                    BasicBlock preBB = bb.getPredecessors().get(0);
                    if (preBB.getSuccessors().size() == 1) {
                        Instr instr = preBB.getLast();
                        if(instr instanceof Jump && ((Jump) instr).getTargetBlock().equals(bb)){
//                            System.out.println(preBB);
                            ArrayList<Instr> temp = new ArrayList<>();
                            for(Instr instr1: bb.getInstrs()){
                                temp.add(instr1);
                            }
                            for(Instr instr1: temp) {
                                bb.getInstrs().remove(instr1);
                                preBB.addInstr(instr1);
                                instr1.setBasicBlock(preBB);
                            }
                            instr.remove();
                            preBB.getSuccessors().remove(bb);
                            if(bb.getSuccessors().size() > 0) {
                                for(int j = 0; j < bb.getSuccessors().size(); j++) {
                                    BasicBlock succ = bb.getSuccessors().get(j);
                                    preBB.getSuccessors().add(succ);
                                    for(int k = 0; k < succ.getPredecessors().size(); k++) {
                                        if(succ.getPredecessors().get(k).equals(bb)){
                                            succ.getPredecessors().set(k, preBB);
                                        }
                                    }
//                                    System.out.println(succ);
                                }
                            }
                            function.getBasicBlocks().remove(bb);
//                            for(Used used: bb.usedInfo) {
//
//                            }
//                            System.out.println(preBB);
//                            System.out.println(function.name);
//                            System.out.println(function.getBasicBlocks().size);
                        }

                    }
                }
            }
        }
    }


    //删除不活跃的基本块
    private class RemoveDeadBB{
        private Function function;
        private RemoveDeadBB(Function function) {
            this.function = function;
        }
        public void run()
        {
            //逆后序遍历
            ArrayList<BasicBlock> reversePostOrderBB = new ReversePostOrder(function).get();
            //标记活跃的BB
            HashMap<BasicBlock, Boolean> liveBB = new HashMap<>();
            for(BasicBlock bb : function.getBasicBlocks()) {
                liveBB.put(bb, false);
            }
            liveBB.put(function.getEntryBlock(), true);
            //逆后序遍历，标记活跃的BB
            for(int i = 0; i < reversePostOrderBB.size(); i++) {
                BasicBlock bb = reversePostOrderBB.get(i);
                if(liveBB.get(bb)) {
                    for(BasicBlock successor : bb.getSuccessors()) {
                        liveBB.put(successor, true);
                    }
                }
            }
            //删除死BB
            for(int i = function.getBasicBlocks().size() - 1; i >= 0; i--) {
                BasicBlock bb = function.getBasicBlocks().get(i);
                if(!liveBB.get(bb)) {
                    bb.getFunction().getBasicBlocks().remove(bb);
                    if(bb.getSuccessors().size()>0){
                        for(BasicBlock successor : bb.getSuccessors()) {
                            successor.getPredecessors().remove(bb);
                        }
                    }
                    if(bb.getPredecessors().size()>0){
                        for(BasicBlock predecessor : bb.getPredecessors()) {
                            predecessor.getSuccessors().remove(bb);
                        }
                    }
                }
            }
        }
    }

    //构造CFG


    //构造支配树
    private class DominatorTree {
        private Function function;

        private DominatorTree(Function function) {
            this.function = function;
        }
        private void run(){
            //定义支配树
            HashMap<BasicBlock, BasicBlock> dominatorTree = new HashMap<>();
            //初始化支配树
            for(BasicBlock bb : function.getBasicBlocks()) {
                dominatorTree.put(bb, null);
                //初始化BasicBlock的DominatorTreeDepth
                bb.setDomTreeDepth(0);
            }
            //构造支配树
            dominatorTree.put(function.getEntryBlock(), function.getEntryBlock());
            boolean changed = true;
            while(changed) {
                changed = false;
                for(BasicBlock bb : new ReversePostOrder(function).get()) {
                    if(bb == function.getEntryBlock()) {
                        continue;
                    }
                    BasicBlock newIDom = null;
                    for(BasicBlock predecessor : bb.getPredecessors()) {
                        if(dominatorTree.get(predecessor) != null) {
                            newIDom = predecessor;
                            break;
                        }
                    }
                    for(BasicBlock predecessor : bb.getPredecessors()) {
                        if(predecessor == newIDom) {
                            continue;
                        }
                        if(dominatorTree.get(predecessor) != null) {
                            newIDom = intersect(predecessor, newIDom,dominatorTree);
                        }
                    }
                    if(dominatorTree.get(bb) != newIDom && bb != newIDom) {
                        dominatorTree.put(bb, newIDom);
                        newIDom.addIDoms(bb);
                        bb.setDomTreeDepth(newIDom.getDomTreeDepth() + 1);
                        changed = true;
                    }
                }
            }
            //将支配树加入到function中
            function.setDomTree(dominatorTree);
        }

        private BasicBlock intersect(BasicBlock bb1, BasicBlock bb2,HashMap<BasicBlock, BasicBlock> dominatorTree) {
            BasicBlock finger1 = bb1;
            BasicBlock finger2 = bb2;
            while(finger1 != finger2) {
                //如何获取一个basicblock的DominatorTreeDepth？
                //在BasicBlock中添加一个DominatorTreeDepth的成员变量
                //在构造支配树的时候，对每个basicblock的DominatorTreeDepth进行初始化
                //在构造支配树的时候，对每个basicblock的DominatorTreeDepth进行更新
                while(finger1.getDomTreeDepth() < finger2.getDomTreeDepth()) {
                    finger2 = dominatorTree.get(finger2);
                }
                while(finger2.getDomTreeDepth() < finger1.getDomTreeDepth()) {
                    finger1 = dominatorTree.get(finger1);
                }
                if(finger1.getDomTreeDepth() == finger2.getDomTreeDepth() && finger1 != finger2){
                    finger1 = dominatorTree.get(finger1);
                    finger2 = dominatorTree.get(finger2);
                }
            }
            return finger1;
        }
    }

    //构造支配边界
    private class DominanceFrontier {
        private Function function;
        private DominanceFrontier(Function function) {
            this.function = function;
        }
        private void run() {
            //定义支配边界
            HashMap<BasicBlock, ArrayList<BasicBlock>> dominanceFrontier = new HashMap<>();
            //初始化支配边界
            for(BasicBlock bb : function.getBasicBlocks()) {
                dominanceFrontier.put(bb, new ArrayList<>());
            }
            //构造支配边界
            for(BasicBlock bb : function.getBasicBlocks()) {
                if(bb.getPredecessors().size() >= 2) {
                    for(BasicBlock predecessor : bb.getPredecessors()) {
                        BasicBlock runner = predecessor;
                        while(runner != function.getDomTree().get(bb)) {
                            dominanceFrontier.get(runner).add(bb);
                            runner = function.getDomTree().get(runner);
                        }
                    }
                }
            }
            //将支配边界加入到function中
            function.setDomFrontier(dominanceFrontier);
        }
    }

    //循环分析
     private class LoopAnalysis {
         private Function function;
         private LoopAnalysis(Function function) {
             this.function = function;
         }
        //循环分析，构造循环树
         private void run() {

         }
     }
    
}
