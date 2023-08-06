package backend;

import lir.McBlock;
import lir.McFunction;
import lir.Operand;
import lir.mcInstr.McInstr;
import lir.mcInstr.McMove;
import util.MyNode;

import java.util.*;
import java.util.stream.Collectors;

public class RegAllocate {
    public ArrayList<McFunction> mcFunctions;
    private int K = 32; // 着色数
    private String type = "Float"; // 寄存器分配类型
    public McFunction curMcFunc;
    private int MAX_DEGREE = Integer.MAX_VALUE >> 2;
    Set<McInstr> workListMoves;
    Set<Edge> adjSet;
    Set<Operand> preColored;

    /**
     * 未做好合并准备的move指令
     */
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
    /**
     * 已经合并的传送指令
     */
    Set<McMove> coalescedMoves;
    /**
     * 相冲突的指令集合
     */
    Set<McMove> constrainedMoves;

    /**
     * 不再考虑合并的move集合
     */
    Set<McMove> frozenMoves;
    /**
     * 本轮溢出的节点集合
     */
    Set<Operand> spilledNodes;



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
    private void init(McFunction mcFunction){
        for(McBlock mcBlock: mcFunction.getMcBlocks()) {
            int succSize = mcBlock.getSuccMcBlocks().size();
            int predSize = mcBlock.getPredMcBlocks().size();
            double weight = Math.pow(10, Math.min(succSize, predSize));
            for(McInstr mcInstr: mcBlock.getMcInstrs()) {
                for(Operand use: mcInstr.useOperands) {
                    if(use.needColor(type)) {
                        use.addWeight(weight);
                    }
                }
                for(Operand def: mcInstr.defOperands) {
                    if(def.needColor(type)) {
                        def.addWeight(weight);
                    }
                }
            }
        }
    }
    private void turnInit(McFunction mcFunction) {
        workListMoves = new HashSet<>();
        preColored = new HashSet<>();
        activeMoves = new HashSet<>();
        spillWorkList = new HashSet<>();
        freezeWorkList = new HashSet<>();
        simplifyWorkList = new HashSet<>();
        selectStack = new Stack<>();
        coalescedNodes =  new HashSet<>();
        coalescedMoves = new HashSet<>();
        constrainedMoves = new HashSet<>();
        frozenMoves = new HashSet<>();
        spilledNodes = new HashSet<>();
    }

    private void allocate(McFunction mcFunction) {
        curMcFunc = mcFunction;
        while(true) {
            // 生存周期分析
            livenessAnalysis();
            turnInit(curMcFunc);
            adjSet = new HashSet<>();
            if(type == "Integer") {
                for(int i = 0; i < K; i++) {
                    Operand.PhyReg.getPhyReg(i).degree = MAX_DEGREE;
                    Operand.PhyReg.getPhyReg(i).setAlias(null);
                }
                for(Operand operand: curMcFunc.vrList) {
                    operand.degree = 0;
                    operand.setAlias(null);
                    operand.adjOpdSet = new HashSet<>();
                    operand.moveList = new HashSet<>();
                }
            } else {
                assert type == "Float";
                for(int i = 0; i < K; i++) {
                    Operand.FPhyReg.getFPhyReg(i).degree = MAX_DEGREE;
                    Operand.FPhyReg.getFPhyReg(i).setAlias(null);
                }
                for(Operand operand: curMcFunc.svrList) {
                    operand.degree = 0;
                    operand.setAlias(null);
                    operand.adjOpdSet = new HashSet<>();
                    operand.moveList = new HashSet<>();
                }
            }


            System.err.println("这里应该缺一堆需要初始化的东西");
            build();
            makeWorkList();
            regAllocIteration();
            assignColors();

            if(spilledNodes.size() == 0) {
                break;
            }

            for(Operand operand: spilledNodes) {
                dealSpillNode(operand);
            }
        }
    }

    private void dealSpillNode(Operand opd) {
        System.err.println("dealSpillNode尚未完成");
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
        Operand u = getAlias(mcMove.defOperands.get(0));
        Operand v = getAlias(mcMove.useOperands.get(0));
        if(preColored.contains(v)) {
            Operand temp = u;
            u = v;
            v = temp;
        }
        workListMoves.remove(mcMove);
        if(u.equals(v)) {
            coalescedMoves.add(mcMove);
            addWorkList(u);
        } else if(preColored.contains(v) || adjSet.contains(new Edge(u, v))) {
            constrainedMoves.add(mcMove);
            addWorkList(u);
            addWorkList(v);
        } else {
            if(preColored.contains(u)) {
                boolean flag = true;
                for(Operand adj: getAdjacent(v)) {
                    if(adj.degree >= K && !preColored.contains(adj) && !adjSet.contains(new Edge(adj, v))){
                        flag = false;
                        break;
                    }
                }
                if(flag) {
                    coalescedMoves.add(mcMove);
                    combine(u, v);
                    addWorkList(u);
                } else {
                    activeMoves.add(mcMove);
                }
            } else {
                Set<Operand> union = getAdjacent(u);
                union.addAll(getAdjacent(v));
                int cnt = 0;
                for(Operand opd: union) {
                    if(!selectStack.contains(opd) && !coalescedNodes.contains(opd) && opd.degree >= K){
                        cnt ++;
                    }
                }
                if(cnt < K) {
                    coalescedMoves.add(mcMove);
                    combine(u, v);
                    addWorkList(u);
                } else {
                    activeMoves.add(mcMove);
                }
            }
        }
    }

    /**
     * 合并u,v
     */
    private void combine(Operand u, Operand v) {
        if(freezeWorkList.contains(v)) {
            freezeWorkList.remove(v);
        } else {
            spillWorkList.remove(v);
        }

        coalescedNodes.add(v);
        v.setAlias(u);
        u.moveList.addAll(v.moveList);

        for(Operand adj: v.adjOpdSet) {
            if(!selectStack.contains(adj) && !coalescedNodes.contains(adj)) {
                addEdge(adj, u);
                decrementDegree(adj);
            }
        }

        if(u.degree >= K && freezeWorkList.contains(u)) {
            freezeWorkList.remove(u);
            spillWorkList.add(u);
        }
    }

    private Set<Operand> getAdjacent(Operand opd) {
        Set<Operand> adjSet = new HashSet<>(opd.adjOpdSet);
        adjSet.removeAll(selectStack);
        adjSet.removeAll(coalescedNodes);
        return adjSet;
    }


    private void addWorkList(Operand opd) {
        if(!preColored.contains(opd) && !isMoveRelated(opd) && opd.degree < K) {
            freezeWorkList.remove(opd);
            simplifyWorkList.add(opd);
        }
    }

    private Operand getAlias(Operand p) {
        while(coalescedNodes.contains(p)) {
            p = p.getAlias();
        }
        return p;
    }

    private void freeze() {
        Operand opd = freezeWorkList.iterator().next();
        freezeWorkList.remove(opd);
        simplifyWorkList.add(opd);
        freezeMoves(opd);
    }

    private void freezeMoves(Operand u) {
        for(McMove mcMove: u.moveList) {
            if(activeMoves.contains(mcMove) || workListMoves.contains(mcMove)) {
                if (activeMoves.contains(mcMove)) {
                    activeMoves.remove(mcMove);
                } else {
                    workListMoves.remove(mcMove);
                }
                frozenMoves.add(mcMove);

                Operand v;
                if (getAlias(mcMove.useOperands.get(0)).equals(getAlias(u))) {
                    v = getAlias(mcMove.defOperands.get(0));
                } else {
                    v = getAlias(mcMove.useOperands.get(0));
                }
                Set<McMove> vMcMove =  v.moveList.stream()
                        .filter(move -> activeMoves.contains(move) || workListMoves.contains(move))
                        .collect(Collectors.toSet());
                if(vMcMove.size() == 0 && v.degree < K) {
                    freezeWorkList.remove(v);
                    simplifyWorkList.add(v);
                }
            }
        }
    }

    private void selectSpill() {
        Operand opd = spillWorkList.iterator().next();
        double cost;
        double min = Double.MAX_VALUE;
        for(Operand operand: spillWorkList) {
            if(operand.degree != 0 && !operand.isRecentSpill()) {
                cost = operand.getWeight() / operand.degree;
            } else {
                cost = Double.MAX_VALUE;
            }
            if(cost < min) {
                min = cost;
                opd = operand;
            }
        }
        spillWorkList.remove(opd);
        simplifyWorkList.add(opd);
        freezeMoves(opd);
    }

    HashMap<Operand, Operand> colorMap;

    private void assignColors() {
        preAssignColors();
    }

    private void preAssignColors() {
        colorMap = new HashMap<>();

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
