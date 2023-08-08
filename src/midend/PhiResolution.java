package midend;

import ir.*;
import ir.instruction.*;
import ir.type.Int32Type;
import util.MyList;

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
        ArrayList<BasicBlock> BBs = new ArrayList<>();
        for(BasicBlock bb:function.getBasicBlocks())
            BBs.add(bb);
        for(BasicBlock bb:BBs)
        {
            System.out.println(bb.getName());
            if(!(bb.getInstrs().get(0) instanceof Phi)) continue;
            ArrayList<BasicBlock> incomings = new ArrayList<>(bb.getPredecessors());
            ArrayList<Pcopy> PCs = new ArrayList<>();
            for(int j = 0;j < incomings.size(); j++){
                System.out.println("incoming:"+incomings.get(j).getName());
                BasicBlock incomingBB = incomings.get(j);
                Pcopy pcopy = new Pcopy();//空的平行复制指令
                if(incomingBB.getSuccessors().size()>1){
                    BasicBlock freshBlock = new BasicBlock(function);
                    replaceEdge(incomingBB, bb, freshBlock);
                    pcopy.setBasicBlock(freshBlock);
                }
                else{
                    pcopy.setBasicBlock(incomingBB);
                }
                PCs.add(pcopy);
            }
            for(Instr instr:bb.getInstrs()){
                if(instr instanceof Phi){
                    Phi phi = (Phi) instr;
                    for(int j = 0; j < phi.getUses().size(); j++){
                        //Variable freshVar = new Variable(phi.getType());
                        PCs.get(j).addFromAndTo(phi.getUse(j), phi);
                        System.out.println("bb:"+PCs.get(j).getBasicBlock().getName()+" "+PCs.get(j));
                        phi.modifyUse(j, phi);
                    }
                }else
                    break;
            }
            for(Instr instr:bb.getInstrs()){
                if(instr instanceof Phi){
                    Phi phi = (Phi) instr;
                    phi.remove();
                }else
                    break;
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

        Instr instr = from.getInstrs().get(from.getInstrs().size()-1);
        Branch branch = (Branch) instr;
        BasicBlock thenBB = (branch).getThenBlock();
        BasicBlock elseBB = (branch).getElseBlock();

        if (to.equals(thenBB)) {
            branch.setThenBlock(freshBlock);
            Jump jump = new Jump(to, freshBlock);
        } else if (to.equals(elseBB)) {
            branch.setElseBlock(freshBlock);
            Jump jump = new Jump(to, freshBlock);
        } else {
            System.err.println("Panic At Remove PHI addMidBB");
        }
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
