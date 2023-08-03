package midend;

import ir.BasicBlock;
import ir.Function;
import ir.GlobalValue;
import ir.instruction.Branch;
import ir.instruction.Instr;
import ir.instruction.Jump;

import java.util.ArrayList;
import java.util.HashMap;

public class DomainAnalysis extends Pass {
    public DomainAnalysis(HashMap<String, Function> functions, ArrayList<GlobalValue> globals) {
        super(functions, globals);
    }
    public void run(){
        for(Function function : this.functions.values()) {
            if(function.isExternal()) {
                continue;
            }
            new ControlFlowGraph(function).run();
            new DominatorTree(function).run();
            // new PostDominatorTree(function).run();
            // new DominanceFrontier(function).run();
            // new LoopAnalysis(function).run();
        }
    }

    //构造CFG
    private class ControlFlowGraph {
        private Function function;
        private ControlFlowGraph(Function function) {
            this.function = function;
        }
        private void run() {
            //定义前驱后继图
            HashMap<BasicBlock, ArrayList<BasicBlock>> predecessors = new HashMap<>();
            HashMap<BasicBlock, ArrayList<BasicBlock>> successors = new HashMap<>();
            //初始化前驱后继图
            for(BasicBlock bb : function.getBasicBlocks()) {
                predecessors.put(bb, new ArrayList<>());
                successors.put(bb, new ArrayList<>());
            }
            //构造前驱后继图
            for(BasicBlock bb : function.getBasicBlocks()) {
                Instr terminator = bb.getLast();
                if(terminator instanceof Branch) {
                    BasicBlock thenBB = ((ir.instruction.Branch) terminator).getThenBlock();
                    BasicBlock elseBB = ((ir.instruction.Branch) terminator).getElseBlock();
                    successors.get(bb).add(thenBB);
                    successors.get(bb).add(elseBB);
                    predecessors.get(thenBB).add(bb);
                    predecessors.get(elseBB).add(bb);
                } else if(terminator instanceof Jump) {
                    BasicBlock targetBB = ((ir.instruction.Jump) terminator).getTargetBlock();
                    successors.get(bb).add(targetBB);
                    predecessors.get(targetBB).add(bb);
                } else {
                    assert terminator instanceof ir.instruction.Return;
                }
            }
            //将前驱后继图加入到function中
            function.setPredecessors(predecessors);
            function.setSuccessors(successors);

            //将前驱后继图加入到BasicBlock中
            for(BasicBlock bb : function.getBasicBlocks()) {
                bb.setPredecessors(predecessors.get(bb));
                bb.setSuccessors(successors.get(bb));
            }
        }
    }

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
                for(BasicBlock bb : function.getBasicBlocks()) {
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
                    if(dominatorTree.get(bb) != newIDom) {
                        dominatorTree.put(bb, newIDom);
                        bb.setDomTreeDepth(0);
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
                    finger1 = dominatorTree.get(finger1);
                }
                while(finger2.getDomTreeDepth() < finger1.getDomTreeDepth()) {
                    finger2 = dominatorTree.get(finger2);
                }
            }
            return finger1;
        }
    }

    //构造后支配树
    
}
