package midend;

import ir.BasicBlock;
import ir.Function;
import ir.GlobalValue;
import ir.instruction.Instr;
import ir.instruction.Jump;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class SimpleCFG extends Pass{
    public SimpleCFG(HashMap<String, Function> functions, ArrayList<GlobalValue> globals) {
        super(functions, globals);
    }
    public void run(){
        for(Function function : this.functions.values()) {
            if(function.isExternal()) {
                continue;
            }
            new RemoveSingleBB(function).run();
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
}
