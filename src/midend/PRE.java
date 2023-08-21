package midend;

import ir.*;
import ir.instruction.*;
import ir.type.Int32Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

public class PRE extends Pass{

    private HashSet<String> allInstr = new HashSet<>();
    private HashMap<String,Instr> str2Instr = new HashMap<>();
    private HashMap<BasicBlock,HashSet<String>> transLoc = new HashMap<>();
    private HashMap<BasicBlock,HashSet<String>> antLoc = new HashMap<>();
    private HashMap<BasicBlock,HashSet<String>> antIn = new HashMap<>();
    private HashMap<BasicBlock,HashSet<String>> antOut = new HashMap<>();
    private HashMap<BasicBlock,HashSet<String>> earlOut = new HashMap<>();
    private HashMap<BasicBlock,HashSet<String>> earlIn = new HashMap<>();
    private HashMap<BasicBlock,HashSet<String>> delayOut = new HashMap<>();
    private HashMap<BasicBlock,HashSet<String>> delayIn = new HashMap<>();
    private HashMap<BasicBlock,HashSet<String>> aneaIn = new HashMap<>();
    private HashMap<BasicBlock,HashSet<String>> lateIn = new HashMap<>();
    private HashMap<BasicBlock,HashSet<String>> isolOut = new HashMap<>();
    private HashMap<BasicBlock,HashSet<String>> isolIn = new HashMap<>();
    private HashMap<BasicBlock,HashSet<String>> opt = new HashMap<>();
    private HashMap<BasicBlock,HashSet<String>> redn = new HashMap<>();

    public PRE(HashMap<String, Function> functions, ArrayList<GlobalValue> globals) {
        super(functions, globals);
    }
    public void run(){
        for(Function function:functions.values()){
            if(function.isExternal()) continue;
            runForFunc(function);
        }
    }
    private void runForFunc(Function function){
        criticalEdgeSplit(function);
        new ControlFlowGraph(function).run();
        calcTransLoc(function);
        calcAntLoc(function);
        calcAntInOut(function);
        calcEarlInOut(function);
        calcDelayInOut(function);
        calcLateIn(function);
        calcIsolInOut(function);
        calcOptRedn(function);
        moveInstr(function);
    }

    private void criticalEdgeSplit(Function function)
    {
        ArrayList<BasicBlock> BBs = new ArrayList<>();
        for(BasicBlock bb:function.getBasicBlocks())
            BBs.add(bb);
        for(BasicBlock bb:BBs)
        {
            if(!(bb.getInstrs().get(0) instanceof Phi)) continue;
            ArrayList<BasicBlock> incomings = new ArrayList<>(bb.getPredecessors());
            for(int j = 0;j < incomings.size(); j++){
                BasicBlock incomingBB = incomings.get(j);
                if(incomingBB.getSuccessors().size()>1){
                    BasicBlock freshBlock = new BasicBlock(function);
                    replaceEdge(incomingBB, bb, freshBlock);
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

        Instr instr = from.getInstrs().getLast();
        //System.out.println(from.getInstrs().size());
        //System.out.println(from);

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

    private void calcTransLoc(Function function){
        for(BasicBlock bb:function.getBasicBlocks()){
            for(Instr instr:bb.getInstrs()){
                if(canMove(instr))
                {
                    String key = getHashofInstr(instr);
                    allInstr.add(key);
                    str2Instr.put(key,instr);
                }
            }
            transLoc.put(bb,new HashSet<>());
        }
        for(BasicBlock bb:transLoc.keySet()){
            transLoc.get(bb).addAll(allInstr);
        }
        for(BasicBlock bb:function.getBasicBlocks()){
            for(String hash:transLoc.get(bb)){
                boolean flag = false;
                for(Value use:str2Instr.get(hash).getUses()){
                    for(Instr instr1:bb.getInstrs()){
                        if(use.equals(str2Instr.get(hash))){
                            flag = true;
                            break;
                        }
                    }
                }
                if(flag){
                    transLoc.get(bb).remove(str2Instr.get(hash));
                }
            }
        }
    }

    private void calcAntLoc(Function function){
        for(BasicBlock bb:function.getBasicBlocks()){
            HashSet<Instr> defInstrs = new HashSet<>();
            HashSet<Value> useValues = new HashSet<>();
            antLoc.put(bb,new HashSet<String>());
            for(Instr instr:bb.getInstrs()){
                if(canMove(instr)){
                    boolean flag = true;
                    //前面用到了该表达式
                    for(Value use:useValues){
                        if(use instanceof Instr && getHashofInstr(((Instr) use))==getHashofInstr(instr)){
                            flag = false;
                            break;
                        }
                    }
                    //前面修改了它的变量
                    for(Value use:instr.getUses()){
                        if(defInstrs.contains(use)){
                            flag = false;
                            break;
                        }
                    }
                    if(flag){
                        antLoc.get(bb).add(getHashofInstr(instr));
                    }
                }
                defInstrs.add(instr);
                for(Value use:instr.getUses()){
                    useValues.add(use);
                }
            }
        }
    }

    private void calcAntInOut(Function function){
        for(BasicBlock bb:function.getBasicBlocks()){
            antOut.put(bb,new HashSet<String>());
            antIn.put(bb,new HashSet<String>());
        }
        boolean changed = true;
        while(changed){
            changed = false;
            for(BasicBlock bb:function.getBasicBlocks()){
                HashSet<String> tempAntOut = antOut.get(bb);
                HashSet<String> tempAntIn = antIn.get(bb);
                if(bb.getSuccessors().size()>0)
                    antOut.get(bb).addAll(antIn.get(bb.getSuccessors().get(0)));
                for(BasicBlock succ:bb.getSuccessors()){
                    antOut.get(bb).retainAll(antIn.get(succ));
                }
                antIn.get(bb).addAll(transLoc.get(bb));
                antIn.get(bb).retainAll(antOut.get(bb));
                antIn.get(bb).addAll(antLoc.get(bb));
                if(!setEqual(tempAntOut,antOut.get(bb)) || !setEqual(tempAntIn,antIn.get(bb)))
                    changed = true;
            }
        }
    }

    private void calcEarlInOut(Function function){
        for(BasicBlock bb:function.getBasicBlocks()){
            earlOut.put(bb,new HashSet<String>());
            earlIn.put(bb,new HashSet<String>());
        }
        BasicBlock entry = function.getBasicBlocks().get(0);
        earlIn.get(entry).addAll(allInstr);
        boolean changed = true;
        while(changed){
            changed = false;
            for(BasicBlock bb:function.getBasicBlocks()){
                HashSet<String> tempOut = earlOut.get(bb);
                HashSet<String> tempIn = earlIn.get(bb);
                if(!bb.equals(entry)){
                    for(BasicBlock pred:bb.getPredecessors()){
                        earlIn.get(bb).addAll(earlOut.get(pred));
                    }
                }
                earlOut.get(bb).addAll(allInstr);
                earlOut.get(bb).removeAll(antIn.get(bb));
                earlOut.get(bb).retainAll(earlIn.get(bb));
                HashSet<String> _transLoc = new HashSet<>();
                _transLoc.addAll(allInstr);
                _transLoc.removeAll(transLoc.get(bb));
                earlOut.get(bb).addAll(_transLoc);

                if(!setEqual(tempOut,earlOut.get(bb)) || !setEqual(tempIn,earlIn.get(bb)))
                    changed = true;
            }
        }
    }

    private void calcDelayInOut(Function function){
        for(BasicBlock bb:function.getBasicBlocks()){
            delayOut.put(bb,new HashSet<String>());
            delayIn.put(bb,new HashSet<String>());
            aneaIn.put(bb,new HashSet<String>());
        }
        BasicBlock entry = function.getBasicBlocks().get(0);
        aneaIn.get(entry).addAll(antIn.get(entry));
        aneaIn.get(entry).retainAll(earlIn.get(entry));
        delayIn.get(entry).addAll(aneaIn.get(entry));
        boolean changed = true;
        while(changed)
        {
            changed = false;
            for(BasicBlock bb:function.getBasicBlocks()){
                HashSet<String> tempOut = delayOut.get(bb);
                HashSet<String> tempIn = delayIn.get(bb);
                if(!bb.equals(entry)){
                    aneaIn.get(bb).addAll(antIn.get(bb));
                    aneaIn.get(bb).retainAll(earlIn.get(bb));
                    HashSet<String> tempSet = new HashSet<>();
                    if(bb.getPredecessors().size()>0){
                        tempSet.addAll(delayOut.get(bb.getPredecessors().get(0)));
                        for(BasicBlock pred:bb.getPredecessors()){
                            tempSet.retainAll(delayOut.get(pred));
                        }
                    }
                    delayIn.get(bb).addAll(aneaIn.get(bb));
                    delayIn.get(bb).addAll(tempSet);
                }
                delayOut.get(bb).addAll(allInstr);
                delayOut.get(bb).removeAll(antLoc.get(bb));
                delayOut.get(bb).retainAll(delayIn.get(bb));
                if(!setEqual(tempOut,delayOut.get(bb)) || !setEqual(tempIn,delayIn.get(bb)))
                    changed = true;
            }
        }
    }

    private void calcLateIn(Function function){
        for(BasicBlock bb:function.getBasicBlocks()){
            lateIn.put(bb,new HashSet<>());
            HashSet<String> tempSet = new HashSet<>();
            if(bb.getSuccessors().size()>0){
                tempSet.addAll(delayIn.get(bb.getSuccessors().get(0)));
            }
            for(BasicBlock succ:bb.getSuccessors()){
                tempSet.retainAll(delayIn.get(succ));
            }
            lateIn.get(bb).addAll(allInstr);
            lateIn.get(bb).removeAll(tempSet);
            lateIn.get(bb).addAll(antLoc.get(bb));
            lateIn.get(bb).retainAll(delayIn.get(bb));
        }
    }

    private void calcIsolInOut(Function function){
        for(BasicBlock bb:function.getBasicBlocks()){
            isolIn.put(bb,new HashSet<String>());
            isolOut.put(bb,new HashSet<String>());
        }
        boolean changed = true;
        while(changed){
            changed = false;
            for(BasicBlock bb:function.getBasicBlocks()){
                HashSet<String> tempIsolOut = isolOut.get(bb);
                HashSet<String> tempIsolIn = isolIn.get(bb);
                if(bb.getSuccessors().size()>0)
                {
                    isolOut.get(bb).addAll(isolIn.get(bb.getSuccessors().get(0)));
                }
                for(BasicBlock succ:bb.getSuccessors()){
                    isolOut.get(bb).retainAll(isolIn.get(succ));
                }

                isolIn.get(bb).addAll(allInstr);
                isolIn.get(bb).removeAll(antLoc.get(bb));
                isolIn.get(bb).retainAll(isolOut.get(bb));
                isolIn.get(bb).addAll(lateIn.get(bb));
                if(!setEqual(tempIsolOut,isolOut.get(bb)) || !setEqual(tempIsolIn,isolIn.get(bb)))
                    changed = true;
            }
        }

    }

    private void calcOptRedn(Function function){
        for(BasicBlock bb:function.getBasicBlocks()){
            opt.put(bb,new HashSet<>());
            redn.put(bb,new HashSet<>());
            opt.get(bb).addAll(allInstr);
            opt.get(bb).removeAll(isolOut.get(bb));
            opt.get(bb).retainAll(lateIn.get(bb));

            HashSet<String> tempSet = new HashSet<>();
            tempSet.addAll(lateIn.get(bb));
            tempSet.retainAll(isolOut.get(bb));
            redn.get(bb).addAll(allInstr);
            redn.get(bb).removeAll(tempSet);
            redn.get(bb).retainAll(antLoc.get(bb));
        }
    }

    private void moveInstr(Function function){
        for(BasicBlock bb :function.getBasicBlocks()){
            System.out.println(bb.getName());
            for(String hash:redn.get(bb)){
                if(!opt.get(bb).contains(hash)){
                    bb.getInstrs().remove(str2Instr.get(hash));
                }
            }
            for(String hash:opt.get(bb)){
                if(!redn.get(bb).contains(hash)){
                    str2Instr.get(hash).setBasicBlock(bb);
                    System.out.println(str2Instr.get(hash));
                    bb.getInstrs().insertBefore(bb.getInstrs().getLast(),str2Instr.get(hash));
                }
            }
        }
    }

    private boolean canMove(Instr instr){
        if(instr instanceof Branch || instr instanceof Jump || instr instanceof Return || instr instanceof Phi || instr instanceof Call || instr instanceof Fcmp || instr instanceof Icmp || instr instanceof Alloc) return false;
        return true;
    }

    private boolean setEqual(HashSet<String> a,HashSet<String> b){
        for(String hash:a){
            if(!b.contains(hash))
                return false;
        }
        for(String hash:b){
            if(!a.contains(hash))
                return false;
        }
        return true;
    }

    private String getHashofInstr(Instr instr){
        if(instr instanceof Binary){
            Binary binary = (Binary) instr;
            if(binary.getType() instanceof Int32Type)
                return binary.getOp().toString() + binary.getLeft().getName() + binary.getRight().getName();
            else{
                return binary.getOp().getfName() + binary.getLeft().getName() + binary.getRight().getName();
            }
        }
        else if(instr instanceof GetElementPtr){
            GetElementPtr getElementPtr = (GetElementPtr) instr;
            String ret = getElementPtr.getPointer().getName();
            for(Value value:getElementPtr.getIdxList()){
                ret += value.getName();
            }
            return ret;
        }
        else if(instr instanceof Call){
            Call call = (Call) instr;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("call");
            stringBuilder.append(call.getFunction().getName());
            for(int i=0;i<call.getParams().size();i++)
            {
                stringBuilder.append(call.getParams().get(i).getName());
                if(i!=call.getParams().size()-1)
                    stringBuilder.append(",");
            }
            return stringBuilder.toString();
        }
        else if(instr instanceof Fptosi){
            Fptosi fptosi = (Fptosi) instr;
            return "Fptosi"+fptosi.getValue().getName();
        }
        else if(instr instanceof Sitofp){
            Sitofp sitofp = (Sitofp) instr;
            return "Sitofp"+sitofp.getValue().getName();
        }
        else if(instr instanceof  BitCast){
            BitCast bitCast = (BitCast) instr;
            return "BitCast"+bitCast.getValue().getName();
        }
        else if(instr instanceof Zext){
            Zext zext = (Zext) instr;
            return "Zext"+zext.getValue().getName();
        }
        else if(instr instanceof Load){
            Load load = (Load) instr;
            return "Load" +load.getPointer().getName();
        }else if(instr instanceof  Store){
            Store store = (Store) instr;
            return "Store"+store.getAddress().getName()+store.getValue().getName();
        }else if(instr instanceof Phi){
            Phi phi = (Phi) instr;
            return "phi"+phi.getUse(0).getName()+phi.getUse(1).getName();
        }else if(instr instanceof Alloc){
            Alloc alloc = (Alloc) instr;
            return "alloc"+alloc.getName();
        }else if(instr instanceof Icmp){
            Icmp icmp = (Icmp) instr;
            return "icmp"+icmp.getRhs().getName()+icmp.getLhs().getName();
        }else if(instr instanceof Fcmp){
            Fcmp fcmp = (Fcmp) instr;
            return "fcmp"+fcmp.getRhs().getName()+fcmp.getLhs().getName();
        }
        else{
            //System.out.println(instr);
            assert false;
            return null;
        }
    }
}
