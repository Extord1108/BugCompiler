package midend;

import ir.BasicBlock;
import ir.Function;
import ir.instruction.Branch;
import ir.instruction.Instr;
import ir.instruction.Jump;

import java.util.ArrayList;
import java.util.HashMap;

public class ControlFlowGraph {
    private Function function;
    public ControlFlowGraph(Function function) {
        this.function = function;
    }
    public void run() {
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
        //System.out.println("pred"+predecessors);
        //System.out.println("succ"+successors);
    }
}
