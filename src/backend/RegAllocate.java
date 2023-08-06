package backend;

import lir.McBlock;
import lir.McFunction;
import lir.Operand;
import lir.mcInstr.McInstr;
import lir.mcInstr.McMove;
import util.MyNode;

import java.util.*;

public class RegAllocate {
    public ArrayList<McFunction> mcFunctions;
    private int K = 32; // 着色数
    private String type = "Float"; // 寄存器分配类型
    public McFunction curMcFunc;
    private int MAX_DEGREE = Integer.MAX_VALUE >> 2;
    Set<McInstr> workListMoves;
    Set<Edge> adjSet;
    Set<Operand> preColored;
    Set<McInstr> activeMoves;

    /**
     * 高度数节点
     */
    Set<Operand> spillWorkList;
    /**
     * 低度数move相关
     */
    Set<Operand> freezeWorkList;
    /**
     * 低度数move无关
     */
    Set<Operand> simplifyWorkList;
    /**
     * 从图中去处的临时寄存器的栈
     */
    Stack<Operand> selectStack;
    /**
     * 已经合并的寄存器集合
     */
    Set<Operand> coalescedNodes;

    public RegAllocate(ArrayList<McFunction> mcFunctions){
        this.mcFunctions = mcFunctions;
    }

    public void alloc(){
        for(McFunction mcFunction: mcFunctions) {
            if(mcFunction.getSvrList().size() > 0) {
                K = 32;
                type = "Float";
                init(mcFunction);
                allocate(mcFunction);
            }
            K = 14;
            type = "Integer";
            init(mcFunction);
            allocate(mcFunction);
        }
    }

    private void init(McFunction mcFunction) {
        workListMoves = new HashSet<>();
        preColored = new HashSet<>();
        activeMoves = new HashSet<>();
        spillWorkList = new HashSet<>();
        freezeWorkList = new HashSet<>();
        simplifyWorkList = new HashSet<>();
        selectStack = new Stack<>();
        coalescedNodes =  new HashSet<>();
    }

    private void allocate(McFunction mcFunction) {
        curMcFunc = mcFunction;
        while(true) {
            // 生存周期分析
            livenessAnalysis();
            adjSet = new HashSet<>();
            if(type == "Integer") {
                for(int i = 0; i < K; i++) {
                    Operand.PhyReg.getPhyReg(i).degree = MAX_DEGREE;
                }
                for(Operand operand: curMcFunc.vrList) {
                    operand.degree = 0;
                    operand.adjOpdSet = new HashSet<>();
                    operand.moveList = new HashSet<>();
                }
            } else {
                assert type == "Float";
                for(int i = 0; i < K; i++) {
                    Operand.FPhyReg.getFPhyReg(i).degree = MAX_DEGREE;
                }
                for(Operand operand: curMcFunc.svrList) {
                    operand.degree = 0;
                    operand.adjOpdSet = new HashSet<>();
                    operand.moveList = new HashSet<>();
                }
            }


            System.err.println("这里应该缺一堆需要初始化的东西");
            build();
            makeWorkList();
            regAllocIteration();
            assignColors();
        }
    }

    private void regAllocIteration() {
        while (!spillWorkList.isEmpty() || !freezeWorkList.isEmpty() || !simplifyWorkList.isEmpty()
                || !workListMoves.isEmpty()) {
            if(!simplifyWorkList.isEmpty()) {
                simplify();
            }
            if(!workListMoves.isEmpty()) {
                coalesce();
            }
            if(!freezeWorkList.isEmpty()) {
                freeze();
            }
            if(!spillWorkList.isEmpty()) {
                selectSpill();
            }
        }
    }

    private void simplify() {
        Operand operand = simplifyWorkList.iterator().next();
        spillWorkList.remove(operand);
        selectStack.push(operand);
        for(Operand adj: operand.adjOpdSet) {
            if(!(selectStack.contains(adj) || coalescedNodes.contains(adj))){
                decrementDegree(adj);
            }
        }
    }

    private void coalesce() {
        McMove mcMove = (McMove) workListMoves.iterator().next();

    }

    private void freeze() {
        System.out.println("freeze尚未完成");
    }

    private void selectSpill() {
        System.out.println("selectSpill尚未完成");
    }

    private void assignColors() {
        System.out.println("assignColors尚未完成");
    }

    private void decrementDegree(Operand opd) {
        opd.degree --;
        if(opd.degree == K - 1) {
            for(McMove mcMove: opd.moveList) {
                if(activeMoves.contains(mcMove)) {
                    activeMoves.remove(mcMove);
                    workListMoves.add(mcMove);
                }
            }
            spillWorkList.remove(opd);
            if(isMoveRelated(opd)) {
                freezeWorkList.add(opd);
            } else {
                simplifyWorkList.add(opd);
            }
        }
    }

    private void makeWorkList() {
        ArrayList<Operand> opList;
        if(type == "Integer")
            opList = curMcFunc.vrList;
        else{
            assert type == "Float";
            opList = curMcFunc.svrList;
        }
        for(Operand opd: opList) {
            if(opd.degree >= K) {
                spillWorkList.add(opd);
            } else if(isMoveRelated(opd)) {
                freezeWorkList.add(opd);
            } else {
                simplifyWorkList.add(opd);
            }
        }
    }

    private boolean isMoveRelated(Operand opd) {
        if(opd.moveList.size() == 0) {
            return false;
        }
        for(McInstr mcInstr: opd.moveList) {
            if(activeMoves.contains(mcInstr) || workListMoves.contains(mcInstr)) {
                return true;
            }
        }
        return false;
    }

    private void build() {
        for(MyNode iter = curMcFunc.getMcLastBlock(); iter !=
                curMcFunc.getMcBlocks().head; iter = iter.getPrev()){
            McBlock mcBlock = (McBlock) iter;
            HashSet<Operand> live = new HashSet<>(mcBlock.liveOutSet);
            for(MyNode myNode = mcBlock.getMcLastInstr(); myNode !=
                    mcBlock.getMcInstrs().head; myNode = myNode.getPrev()) {
                McInstr mcInstr = (McInstr) myNode;
                if(mcInstr instanceof McMove && ((McMove) mcInstr).getSrcOp().needColor(type)
                        && ((McMove) mcInstr).getDstOp().needColor(type)) {
                    McMove mcMove = (McMove) mcInstr;
                    live.remove(mcMove.getSrcOp());
                    mcMove.getDstOp().moveList.add(mcMove);
                    mcMove.getSrcOp().moveList.add(mcMove);
                    workListMoves.add(mcMove);
                }
                dealDefUse(live, mcInstr, mcBlock);
            }
        }
    }

    private void dealDefUse(HashSet<Operand> live, McInstr mcInstr, McBlock mcBlock) {
        ArrayList<Operand> defs = mcInstr.defOperands;
        ArrayList<Operand> uses = mcInstr.useOperands;
        for(Operand def: defs) {
            if(def.needColor(type)) {
                live.add(def);
            }
        }

        for(Operand def: defs) {
            if(def.needColor(type)) {
                for(Operand operand: live) {
                    addEdge(def, operand);
                }
            }
        }

        for(Operand def: defs) {
            if(def.needColor(type)) {
                live.remove(def);
            }
        }

        for(Operand use: uses) {
            if(use.needColor(type)) {
                live.add(use);
            }
        }

    }

    private void livenessAnalysis() {
        for(McBlock mcBlock: curMcFunc.getMcBlocks()){
            mcBlock.liveUseSet = new HashSet<>();
            mcBlock.liveDefSet = new HashSet<>();
            for(McInstr mcInstr : mcBlock.getMcInstrs()) {
                // 在这个block中先被use
                for(Operand use: mcInstr.useOperands) {
                    if(use.needColor(type) && !mcBlock.liveDefSet.contains(use)){
                        mcBlock.liveUseSet.add(use);
                    } else{
                        assert true;
                    }
                }
                // 在这个block中写被def
                for(Operand def: mcInstr.defOperands) {
                    if(def.needColor(type) && !mcBlock.liveUseSet.contains(def)) {
                        mcBlock.liveDefSet.add(def);
                    } else {
                        assert true;
                    }
                }
            }
            mcBlock.liveInSet.addAll(mcBlock.liveUseSet);
            mcBlock.liveOutSet = new HashSet<>();
        }
        liveInOutAnalysis();
    }

    private void liveInOutAnalysis() {
        boolean changed = true;
        while (changed){
            changed = false;
            for(MyNode iter = curMcFunc.getMcLastBlock(); iter !=
                    curMcFunc.getMcBlocks().head; iter = iter.getPrev()){
                McBlock mcBlock = (McBlock) iter;
                HashSet<Operand> newLiveOut = new HashSet<>();
                for(McBlock succMcBlock: mcBlock.getSuccMcBlocks()) {
                    for(Operand liveIn : succMcBlock.liveInSet) {
                        if(!mcBlock.liveOutSet.contains(liveIn)) {
                            newLiveOut.add(liveIn);
                        }
                    }
                }
                if(newLiveOut.size() > 0) {
                    changed = true;
                }
                if(changed) {
                    for(Operand operand: mcBlock.liveOutSet) {
                        if(!mcBlock.liveDefSet.contains(operand)) {
                            mcBlock.liveInSet.add(operand);
                        }
                    }
                }
            }
        }
    }

    private void addEdge(Operand u, Operand v) {
        Edge edge = new Edge( u, v);
        if(!adjSet.contains(edge) && !u.equals(v)) {
            adjSet.add(edge);
            if(!preColored.contains(u)) {
                u.addAdj(v);
                u.degree ++;
            }

            if(!preColored.contains(v)) {
                v.addAdj(u);
                v.degree ++;
            }
        }
    }

    public static class Edge{
        public Operand u;
        public Operand v;

        public Edge(Operand u, Operand v) {
            this.u = u;
            this.v  = v;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Edge edge = (Edge) o;
            return (Objects.equals(u, edge.u) && Objects.equals(v, edge.v)) ||
                    (Objects.equals(v, edge.u) && Objects.equals(u, edge.v));
        }

        @Override
        public int hashCode() {
            return Objects.hash(u, v);
        }
    }
}
