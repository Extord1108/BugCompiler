package midend;

import ir.*;
import ir.instruction.Instr;
import ir.instruction.Move;
import ir.instruction.Pcopy;
import ir.instruction.Phi;
import ir.type.Int32Type;

import java.util.ArrayList;
import java.util.HashMap;

public class PhiResolution extends Pass {

    public PhiResolution(HashMap<String, Function> functions, ArrayList<GlobalValue> globals) {
        super(functions, globals);
    }

    @Override
    public void run()
    {
        for(Function function : functions.values())
        {
            if(function.isExternal()) continue;
            criticalEdgeSplit(function);//分裂临界边，作用是将非传统SSA转为传统SSA
            replacePCwithSC(function);//将Parallel copy 变为 sequence copy
        }
    }

    private void criticalEdgeSplit(Function function)
    {
        for(int i = 0; i < function.getBasicBlocks().size(); i++)
        {
            BasicBlock bb = function.getBasicBlocks().get(i);
            ArrayList<BasicBlock> incomings = bb.getPredecessors();
            ArrayList<Pcopy> PCs = new ArrayList<>();
            for(int j = 0;j < incomings.size(); j++){
                BasicBlock incomingBB = incomings.get(j);
                Pcopy pcopy = new Pcopy();//空的平行复制指令
                PCs.add(pcopy);
                if(incomingBB.getSuccessors().size()>1){
                    BasicBlock freshBlock = new BasicBlock();
                    replaceEdge(incomingBB, bb, freshBlock);
                    pcopy.setBasicBlock(freshBlock);
                }
                else{
                    pcopy.setBasicBlock(incomingBB);
                }
            }
            for(Instr instr:bb.getInstrs()){
                if(instr instanceof Phi){
                    Phi phi = (Phi) instr;
                    for(int j = 0; j < phi.getUses().size(); j++){
                        //Variable freshVar = new Variable(phi.getType());
                        PCs.get(j).addFromAndTo(phi.getUse(j), phi);
                        phi.modifyUse(j, phi);
                        phi.remove();
                    }
                }
            }
        }
    }

    private void replaceEdge(BasicBlock from, BasicBlock to, BasicBlock freshBlock){
        from.getSuccessors().remove(to);
        from.getSuccessors().add(freshBlock);
        freshBlock.getPredecessors().add(from);
        freshBlock.getSuccessors().add(to);
        to.getPredecessors().remove(from);
        to.getPredecessors().add(freshBlock);
    }

    private void replacePCwithSC(Function function) {
        for (BasicBlock bb : function.getBasicBlocks()) {
            for (Instr instr: bb.getInstrs()) {
                if (instr instanceof Pcopy) {
                    Pcopy pcopy = (Pcopy) instr;
                    ArrayList<Instr> seq = new ArrayList<>();
                    //删除undef的pcopy
                    ArrayList<Value> tempFrom = new ArrayList<>();
                    ArrayList<Value> tempTo = new ArrayList<>();
                    for(int i=0;i<pcopy.getSize();i++)
                    {
                        if(pcopy.getFrom(i).getType()==null) continue;
                        else{
                            tempFrom.add(pcopy.getFrom(i));
                            tempTo.add(pcopy.getTo(i));
                        }
                    }
                    pcopy.setFrom(tempFrom);
                    pcopy.setTo(tempTo);
                    while(!checkFromEqualTo(pcopy)){
                        boolean flag = false;
                        Value from = null;
                        Value to = null;
                        for(int i = 0; i < pcopy.getSize(); i++){
                            from = pcopy.getFrom(i);
                            to = pcopy.getTo(i);
                            if(!pcopy.fromContains(to)){
                                flag = true;
                                //TODO: add new copy to seq
                                seq.add(new Move(from.getType(), from, to, bb));
                                pcopy.removeFromAndTo(i);
                                break;
                            }
                        }
                        if(!flag){
                            assert !from.equals(to);
                            Variable freshVar = new Variable(from.getType());
                            //TODO: add new copy to seq
                            seq.add(new Move(from.getType(), from, freshVar, bb));
                            int index = pcopy.getFromIndex(from);
                            pcopy.modifyFrom(index, freshVar);
                            break;
                        }
                    }
                    instr.remove();
                    for(Instr newInstr : seq){
                        Instr LastInstr = bb.getInstrs().getLast();
                        bb.getInstrs().insertBefore(LastInstr,newInstr);
                    }
                }
            }
        }
    }

    private boolean checkFromEqualTo(Instr instr){
        assert instr instanceof Pcopy;
            Pcopy pcopy = (Pcopy) instr;
            for(int i = 0; i < pcopy.getSize(); i++){
                if(!pcopy.getFrom(i).equals(pcopy.getTo(i))){
                    return false;
                }
            }
        return true;
    }
}
