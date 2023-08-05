package midend;

import ir.BasicBlock;
import ir.Function;
import ir.GlobalValue;
import ir.Used;
import ir.Value;
import ir.Variable;
import ir.instruction.Instr;
import ir.instruction.Load;
import ir.instruction.Phi;
import ir.instruction.Store;
import ir.type.FloatType;
import ir.type.Int32Type;
import ir.type.PointerType;
import util.MyList;
import ir.instruction.Alloc;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

public class Mem2Reg extends Pass {
    public Mem2Reg(HashMap<String, Function> functions, ArrayList<GlobalValue> globals) {
        super(functions, globals);
    }

    // 实现一个mem2reg
    @Override
    public void run() {
        for (Function function : this.functions.values()) {
            if (function.isExternal()) {
                continue;
            }
            new Mem2RegAnalysis(function).run();
        }
    }

    private class Mem2RegAnalysis {
        private Function function;

        private Mem2RegAnalysis(Function function) {
            this.function = function;
        }

        private void run() {
            for (BasicBlock bb : function.getBasicBlocks()) {
                for (Instr instr : bb.getInstrs()) {
                    if (instr instanceof Alloc && !(((Alloc) instr).isArrayAlloc())) {
                        // 填充allocInfo
                        Alloc alloc = (Alloc) instr;
                        AllocInfo allocInfo = new AllocInfo(alloc);
                        MyList<Used> usedInfo = alloc.getUsedInfo();
                        for (Used used : usedInfo) {
                            if (used.getUser() instanceof Store) {
                                Store store = (Store) used.getUser();
                                allocInfo.defInstrs.add(store);
                                allocInfo.defBBs.add(store.getBasicBlock());
                            } else if (used.getUser() instanceof Load) {
                                Load load = (Load) used.getUser();
                                allocInfo.useInstrs.add(load);
                                allocInfo.useBBs.add(load.getBasicBlock());
                            } else {
                                throw new RuntimeException("Mem2RegAnalysis: unexpected user of alloc");
                            }
                        }
                        if (allocInfo.useBBs.isEmpty()) {// 如果没有use，那么就直接删掉
                            for (Instr store : allocInfo.defInstrs) {
                                store.remove();
                            }
                        } else if (allocInfo.defBBs.size() == 1) {
                            // 只有一个defineblock，一般情况下它会支配所有的useblock,不用插入phi
                            // 但是可能存在一种情况，就是这个alloc是在循环里面的
                            // 所以我们要判断一下
                            BasicBlock defBB = allocInfo.defBBs.iterator().next();
                            boolean isDom = true;
                            for (BasicBlock useBB : allocInfo.useBBs) {
                                BasicBlock predBlock = useBB;
                                while (predBlock != defBB) {
                                    if (predBlock == null) {
                                        isDom = false;
                                        break;
                                    }
                                    predBlock = function.getDomTree().get(predBlock);
                                }
                                if (!isDom) {
                                    break;
                                }
                            }
                            if (isDom) {
                                Iterator<Instr> iter = defBB.getInstrs().iterator();

                                Instr tempDef = null;
                                while (iter.hasNext()) {
                                    Instr instrofDefBB = iter.next();
                                    if (allocInfo.defInstrs.contains(instrofDefBB)) {
                                        tempDef = instrofDefBB;
                                    } else if (allocInfo.useInstrs.contains(instrofDefBB)) {
                                        if (tempDef != null) {
                                            instrofDefBB.repalceUseofMeto(((Store) tempDef).getValue());
                                        } else {
                                            instrofDefBB.repalceUseofMeto((Variable.getDefaultZero(null)));
                                        }
                                    }
                                }

                                for (Instr instrofUseBB: allocInfo.useInstrs) {
                                    if (!instrofUseBB.getBasicBlock().equals(defBB)) {
                                        assert tempDef != null;
                                        instrofUseBB.repalceUseofMeto(((Store) tempDef).getValue());
                                    }
                                }
                            } else {
                                throw new RuntimeException("Mem2RegAnalysis: unexpected defBB");
                            }
                        } else {// 其他所有情况
                            HashSet<BasicBlock> F = new HashSet<>();
                            HashSet<BasicBlock> worklist = new HashSet<>();
                            HashMap<BasicBlock,Value> inWorklist = new HashMap<>();
                            HashMap<BasicBlock,Value> inserted = new HashMap<>();

                            for(BasicBlock tempbb : function.getBasicBlocks()){
                                inWorklist.put(tempbb,null);
                                inserted.put(tempbb,null);
                            }

                            for(BasicBlock defBB : allocInfo.defBBs){
                                worklist.add(defBB);
                                inWorklist.put(defBB,null);
                            }

                            while (!worklist.isEmpty()) {
                                BasicBlock node = worklist.iterator().next();
                                worklist.remove(node);
                                for (BasicBlock df : function.getDomFrontier().get(node)) {
                                    if(inserted.get(df) != alloc){
                                        //TODO:计算活跃块，只在活跃块中插入phi
                                        Instr phi = null;
                                        ArrayList<Value> args = new ArrayList<>();
                                        for (BasicBlock pred : df.getPredecessors()) {
                                            args.add(new Instr());
                                        }
                                        if (((PointerType) instr.getType()).getBasicType() == FloatType.getInstance()) {
                                            phi = new Phi(FloatType.getInstance(), df, args);
                                        } else {
                                            phi = new Phi(Int32Type.getInstance(), df, args);
                                        }
                                        allocInfo.defInstrs.add(phi);
                                        allocInfo.useInstrs.add(phi);
                                        /********************/
                                        inserted.put(df,alloc);
                                        if(inWorklist.get(df) !=alloc){
                                            worklist.add(df);
                                            inWorklist.put(df,alloc);
                                        }
                                    }
                                }
                            }
                            //rename
                            Stack<Value> stack = new Stack<>();
                            RenameDFS(stack, instr.getBasicBlock().getFunction().getEntryBlock(), allocInfo, function);
                        }
                        instr.remove();
                        if (!allocInfo.useInstrs.isEmpty()) {
                            for (Instr use : allocInfo.useInstrs) {
                                if (!(use instanceof Phi))
                                {
                                    use.remove();
                                }
                            }
                            for (Instr def : allocInfo.defInstrs) {
                                if (!(def instanceof Phi))
                                    def.remove();
                            }
                        }

                    }
                }
            }
        }
    }

    private class AllocInfo {
        private Alloc alloc;
        private HashSet<Instr> defInstrs = new HashSet<>();
        private HashSet<Instr> useInstrs = new HashSet<>();
        private HashSet<BasicBlock> defBBs = new HashSet<>();
        private HashSet<BasicBlock> useBBs = new HashSet<>();

        private AllocInfo(Alloc alloc) {
            this.alloc = alloc;
        }

        @Override
        public String toString() {
            return "AllocInfo{" +
                    "alloc=" + alloc +
                    ", defInstrs=" + defInstrs +
                    ", useInstrs=" + useInstrs +
                    ", defBBs=" + defBBs +
                    ", useBBs=" + useBBs +
                    '}';
        }
    }

    // 用于phi的rename
    public void RenameDFS(Stack<Value> S, BasicBlock X,AllocInfo allocInfo,Function function) {
        int cnt = 0;
        for(Instr instr : X.getInstrs()){
            if (!(instr instanceof Phi) && allocInfo.useInstrs.contains(instr)) {
                instr.repalceUseofMeto(S.empty() ? Variable.getDefaultZero(null) : S.peek());
            }
            if (allocInfo.defInstrs.contains(instr)) {
                if (instr instanceof Store) {
                    S.push(((Store) instr).getValue());
                } else {
                    S.push(instr);
                }
                cnt++;
            }
        }
        ArrayList<BasicBlock> Succ = X.getSuccessors();
        for (int i = 0; i < Succ.size(); i++) {
            BasicBlock bb = Succ.get(i);
            for(Instr instr:Succ.get(i).getInstrs()) {
                if (!(instr instanceof Phi)) {
                    break;
                }
                if (allocInfo.useInstrs.contains(instr)) {
                    ((Phi) instr).modifyUse(bb.getPredecessors().indexOf(X), S.empty() ? Variable.getDefaultZero(null) : S.peek());
                }
                instr = (Instr) instr.getNext();
            }
        }
        for(BasicBlock domee: X.getIDoms()){
            RenameDFS(S,domee,allocInfo,function);
        }
        for (int i = 0; i < cnt; i++) {
            S.pop();
        }
    }
}
