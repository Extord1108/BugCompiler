package backend;

import ir.instruction.Return;
import lir.McBlock;
import lir.McFunction;
import lir.Operand;
import lir.mcInstr.*;
import util.MyList;
import util.MyNode;
import util.OutputHandler;

import java.io.OutputStream;
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

    int count;



    public RegAllocate(ArrayList<McFunction> mcFunctions){
        this.mcFunctions = mcFunctions;
    }

    public void alloc(){
        int t = 0;
        for(McFunction mcFunction: mcFunctions) {
            if(mcFunction.getSvrList().size() > 0) {
                K = 32;
                type = "Float";
                for(int i = 0; i < K; i++) {
                    Operand.FPhyReg.getFPhyReg(i).isAllocated = false;
                }
                init(mcFunction);
                allocate(mcFunction);
            }
            K = 14;
            type = "Integer";
            for(int i = 0; i < K; i++) {
                Operand.PhyReg.getPhyReg(i).isAllocated = false;
            }
            init(mcFunction);
            allocate(mcFunction);
        }
    }
    private void init(McFunction mcFunction){
        preColored = new LinkedHashSet<>();
        for(McBlock mcBlock: mcFunction.getMcBlocks()) {
            int succSize = mcBlock.getSuccMcBlocks().size();
            int predSize = mcBlock.getPredMcBlocks().size();
            double weight = Math.pow(10, Math.min(succSize, predSize));
            for(McInstr mcInstr: mcBlock.getMcInstrs()) {

                for(Operand use: mcInstr.useOperands) {
                    if(use.needColor(type)) {
                        use.addWeight(weight);
                    }
                    if(use.isPreColored(type)) {
                        preColored.add(use);
                    }
                }
                for(Operand def: mcInstr.defOperands) {
                    if(def.needColor(type)) {
                        def.addWeight(weight);
                    }
                    if(def.isPreColored(type)) {
                        preColored.add(def);
                    }
                }
            }
        }

        count = 0;
    }
    private void turnInit(McFunction mcFunction) {
        workListMoves = new LinkedHashSet<>();
        activeMoves = new LinkedHashSet<>();
        spillWorkList = new LinkedHashSet<>();
        freezeWorkList = new LinkedHashSet<>();
        simplifyWorkList = new LinkedHashSet<>();
        selectStack = new Stack<>();
        coalescedNodes =  new LinkedHashSet<>();
        coalescedMoves = new LinkedHashSet<>();
        constrainedMoves = new LinkedHashSet<>();
        frozenMoves = new LinkedHashSet<>();
        spilledNodes = new LinkedHashSet<>();

    }

    private void allocate(McFunction mcFunction) {
        curMcFunc = mcFunction;
        int t = 0;
        while(true) {
            // 生存周期分析
//            if(mcFunction.getName().equals("max_flow")) {
////                System.out.println(t);
//                OutputStream out = OutputHandler.getOutputFile("bug" + t++);
//                OutputHandler.output2Stream(mcFunction.toString(),out);
////                System.out.println("-------------------");
//            }


            livenessAnalysis();
            turnInit(curMcFunc);
            adjSet = new LinkedHashSet<>();
            if(type == "Integer") {
                for(int i = 0; i < K; i++) {
                    Operand.PhyReg.getPhyReg(i).degree = MAX_DEGREE;
                    Operand.PhyReg.getPhyReg(i).setAlias(null);
                    Operand.PhyReg.getPhyReg(i).adjOpdSet = new LinkedHashSet<>();
                    Operand.PhyReg.getPhyReg(i).moveList = new LinkedHashSet<>();
                }
                for(Operand operand: curMcFunc.vrList) {
                    operand.degree = 0;
                    operand.setAlias(null);
                    operand.adjOpdSet = new LinkedHashSet<>();
                    operand.moveList = new LinkedHashSet<>();
                }
            }
            else {
                assert type == "Float";
                for(int i = 0; i < K; i++) {
                    Operand.FPhyReg.getFPhyReg(i).degree = MAX_DEGREE;
                    Operand.FPhyReg.getFPhyReg(i).setAlias(null);
                    Operand.FPhyReg.getFPhyReg(i).adjOpdSet = new LinkedHashSet<>();
                    Operand.FPhyReg.getFPhyReg(i).moveList = new LinkedHashSet<>();
                }
                for(Operand operand: curMcFunc.svrList) {
                    operand.degree = 0;
                    operand.setAlias(null);
                    operand.adjOpdSet = new LinkedHashSet<>();
                    operand.moveList = new LinkedHashSet<>();
                }
            }

            build();
            makeWorkList();
            regAllocIteration();
            assignColors();

            if(spilledNodes.size() == 0) {
                break;
            }
            dealSpillNode();
        }
    }



    private void dealSpillNode() {
        HashSet<Operand> newTemps = new LinkedHashSet<>();
        for(McBlock mcBlock: curMcFunc.getMcBlocks()) {
            ArrayList<McInstr> newInstrs = new ArrayList<>();
            for(McInstr mcInstr: mcBlock.getMcInstrs()) {
//                if(mcInstr instanceof McCall) {
//                    newInstrs.add(mcInstr);
//                    continue;
//                }

                ArrayList<Operand> defs = mcInstr.defOperands;
                ArrayList<Operand> uses = mcInstr.useOperands;
                ArrayList<McInstr> stores = new ArrayList<>();
                ArrayList<McInstr> loads = new ArrayList<>();
                ArrayList<McInstr> storeMove = new ArrayList<>();
                ArrayList<McInstr> loadMove = new ArrayList<>();
                for(Operand opd: spilledNodes) {
                    if(!defs.isEmpty()) {
                        Operand def = defs.get(0);
                        if(opd.equals(def)) {
                            Operand temp = new Operand.VirtualReg( opd.isFloat(),curMcFunc);
                            temp.setRecentSpill(true);
                            newTemps.add(temp);
                            count++;
                            if(opd.getStackPos() != -1) {
                                int stackSize = opd.getStackPos();
                                Operand offset = null;
                                if(stackSize > 4095) {
                                    Operand offTemp = new Operand.VirtualReg( false,curMcFunc);
                                    offTemp.setRecentSpill(true);
                                    if(type == "Integer") {
                                        newTemps.add(offTemp);
                                    }
                                    count ++;
                                    offset = new Operand.Imm(stackSize);
                                    McInstr mcMove = new McMove(offTemp, offset, mcBlock, false);
                                    offset = offTemp;
                                    storeMove.add(mcMove);
                                } else {
                                    offset = new Operand.Imm(stackSize);
                                }
                                McStore mcStore = new McStore(temp, Operand.PhyReg.getPhyReg("sp"),
                                        offset, mcBlock, false);
                                mcInstr.defOperands.set(0, temp);
                                stores.add(mcStore);
                            } else {
                                Operand offset = null;
                                if(curMcFunc.getStackSize() > 4095) {
                                    Operand offTemp = new Operand.VirtualReg( false,curMcFunc);
                                    offTemp.setRecentSpill(true);
                                    if(type == "Integer") {
                                        newTemps.add(offTemp);
                                    }
                                    count ++;
                                    offset = new Operand.Imm(curMcFunc.getStackSize());
                                    McInstr mcMove = new McMove(offTemp, offset, mcBlock, false);
                                    offset = offTemp;
                                    storeMove.add(mcMove);
                                } else {
                                    offset = new Operand.Imm(curMcFunc.getStackSize());
                                }
                                McStore mcStore = new McStore(temp, Operand.PhyReg.getPhyReg("sp"),
                                        offset, mcBlock, false);
                                opd.setStackPos(curMcFunc.getStackSize());
                                curMcFunc.addStackSize(4);
                                mcInstr.defOperands.set(0, temp);
                                stores.add(mcStore);

                            }
                        }
                    }
//                    uses = (ArrayList<Operand>) uses.stream().distinct().collect(Collectors.toList());
                    for(int i = 0; i < uses.size(); i++) {
                        Operand use = uses.get(i);
                        if(opd.equals(use)) {
                            Operand temp = new Operand.VirtualReg(opd.isFloat(),curMcFunc);
                            temp.setRecentSpill(true);
                            newTemps.add(temp);
                            Operand offset = null;
                            if(opd.getStackPos() > 4095) {
                                Operand offTemp = new Operand.VirtualReg( false,curMcFunc);
                                offTemp.setRecentSpill(true);
                                if(type == "Integer") {
                                    newTemps.add(offTemp);
                                }
                                count ++;
                                offset = new Operand.Imm(opd.getStackPos());
                                McInstr move = new McMove(offTemp, offset, mcBlock, false);
                                loadMove.add(move);
                                offset = offTemp;
                            } else {
                                if(opd.getStackPos() == -1) {
                                    int stackPos = curMcFunc.getStackSize();
                                    curMcFunc.addStackSize(4);
                                    if (stackPos > 4095) {
                                        Operand offTemp = new Operand.VirtualReg( false,curMcFunc);
                                        offTemp.setRecentSpill(true);
                                        if(type == "Integer") {
                                            newTemps.add(offTemp);
                                        }
                                        count ++;
                                        offset = new Operand.Imm(opd.getStackPos());
                                        McInstr move = new McMove(offTemp, offset, mcBlock, false);
                                        loadMove.add(move);
                                        offset = offTemp;
                                    } else {
                                        offset = new Operand.Imm(stackPos);
                                    }
                                    opd.setStackPos(stackPos);
                                } else {
                                    offset = new Operand.Imm(opd.getStackPos());
                                }
                            }
                            McInstr mcLoad = new McLdr(temp, Operand.PhyReg.getPhyReg("sp"),
                                    offset, mcBlock, false);
                            mcInstr.useOperands.set(i, temp);
                            loads.add(mcLoad);
                        }
                    }
                }
                if(mcInstr instanceof McMove && mcInstr.useOperands.get(0).equals(mcInstr.defOperands.get(0))) {
                    continue;
                }
                for(McInstr move: loadMove) {
                    newInstrs.add(move);
                }
                for(McInstr load: loads) {
                    newInstrs.add(load);
                }
                newInstrs.add(mcInstr);
                for(McInstr move: storeMove) {
                    newInstrs.add(move);
                }
                for(McInstr store: stores) {
                    newInstrs.add(store);
                }
            }
            MyList<McInstr> mcInstrs = new MyList<>();
            for(McInstr mcInstr: newInstrs) {
                mcInstrs.insertTail(mcInstr);
            }
            mcBlock.setMcInstrs(mcInstrs);
        }
    }

    private void regAllocIteration() {
        while (!spillWorkList.isEmpty() || !freezeWorkList.isEmpty() || !simplifyWorkList.isEmpty()
                || !workListMoves.isEmpty()) {
//            System.out.println( simplifyWorkList.size() + " " + workListMoves.size()+
//                    " " + freezeWorkList.size() + " " + spillWorkList.size());
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
        simplifyWorkList.remove(operand);
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
        Set<Operand> adjSet = new LinkedHashSet<>(opd.adjOpdSet);
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
        if(spilledNodes.size() > 0){
            return;
        }

        for(Operand v : coalescedNodes) {
            Operand a = getAlias(v);
            if(a.needColor(type)) {
                if(preColored.contains(a)) {
                    colorMap.put(v, a.getPhyReg());
                } else {
                    colorMap.put(v, colorMap.get(a));
                }
            }
        }

        ArrayList<McBinary> needFixed = new ArrayList<>();
        for(McBlock mcBlock: curMcFunc.getMcBlocks()) {
//            System.out.println(mcBlock.getName() + ":");
            for(McInstr mcInstr: mcBlock.getMcInstrs()) {
//                System.out.println("\t" + mcInstr);
                if(mcInstr instanceof McBinary && ((McBinary) mcInstr).needFix){
                    needFixed.add((McBinary) mcInstr);
                }
                if(mcInstr instanceof McCall) {
                    curMcFunc.setUseLr();
                    ArrayList<Operand> defs = mcInstr.defOperands;
                    int i = 0;
                    for(Operand def: defs) {
                        defs.set(i ++, def.getPhyReg());
                        def.getPhyReg().isAllocated = true;
                    }
                    i = 0;
                    ArrayList<Operand> uses = mcInstr.useOperands;
                    for(Operand use: uses){
                        uses.set(i ++, use.getPhyReg());
                        use.getPhyReg().isAllocated = true;
                    }
                    continue;
                }
                ArrayList<Operand> defs = mcInstr.defOperands;
                ArrayList<Operand> uses = mcInstr.useOperands;
                if(!defs.isEmpty()) {
                    Operand def = defs.get(0);
                    if(preColored.contains(def) && def.needColor(type)) {
                        defs.set(0, def.getPhyReg());
                        def.getPhyReg().isAllocated = true;
                    } else {
                        Operand set = colorMap.get(def);
                        if(set != null) {
                            if(set instanceof Operand.FPhyReg) {
                                curMcFunc.usedFPhyRegs.add((Operand.FPhyReg) set);
                                defs.set(0, set);
                                defs.get(0).isAllocated = true;
                            } else {
                                assert set instanceof Operand.PhyReg;
                                curMcFunc.usedPhyRegs.add((Operand.PhyReg) set);
                                defs.set(0, set);
                                defs.get(0).isAllocated = true;
                            }
                        }
                    }
                }

                for(int i = 0; i < uses.size(); i++) {
                    Operand use = uses.get(i);
                    if(preColored.contains(use) && use.needColor(type)) {
                        uses.set(i, use.getPhyReg());
                        uses.get(i).isAllocated = true;
                    } else {
                        Operand set = colorMap.get(use);
                        if(set != null) {
                            if(set instanceof Operand.FPhyReg) {
                                curMcFunc.usedFPhyRegs.add((Operand.FPhyReg) set);
                                uses.set(i, set);
                                uses.get(i).isAllocated = true;
                            } else {
                                assert set instanceof Operand.PhyReg;
                                curMcFunc.usedPhyRegs.add((Operand.PhyReg) set);
                                uses.set(i, set);
                                uses.get(i).isAllocated = true;
                            }
                        }
                    }
                }

            }
        }

        fixStack(needFixed);
    }

    private void fixStack(ArrayList<McBinary> needFixed) {
//        System.out.println(curMcFunc.getName());
//        System.out.println(curMcFunc.getStackSize());
        for(McBinary mcBinary: needFixed) {
            int offset;
            if(mcBinary.fixType.equals(McBinary.FixType.VAR_STACK)) {
                offset = curMcFunc.getStackSize();
            } else {
                assert mcBinary.fixType.equals(McBinary.FixType.PARAM_STACK);
                offset = curMcFunc.getStackSize() + mcBinary.getOffset() + curMcFunc.getParamSize();
                int idx = 0;
                int size = 0;
                for(Operand reg: curMcFunc.usedPhyRegs) {
                    idx ++;
                    if(((Operand.PhyReg)reg).getIdx() > 3 && !reg.equals(Operand.PhyReg.getPhyReg("sp"))){
                        size++;
                    }
                }
                idx = 0;
                for(Operand reg: curMcFunc.usedFPhyRegs) {
                    idx ++;
                    if(((Operand.FPhyReg)reg).getIdx() > 15) {
                        size ++;
                    }
                }
                offset = offset + size * 4;
            }
            if(offset == 0) {
                mcBinary.mcBlock.getMcInstrs().remove(mcBinary);
            } else {
                if(CodeGen.canImmSaved(offset)) {
                    mcBinary.useOperands.set(1, new Operand.Imm(offset));
//                    System.out.println(mcBinary);
                } else {
                    Operand off = Operand.PhyReg.getPhyReg("r4");
                    McMove mcMove = new McMove(off, new Operand.Imm(offset), mcBinary.mcBlock, false);
                    mcBinary.mcBlock.getMcInstrs().insertBefore(mcBinary, mcMove);
                    mcBinary.useOperands.set(1, off);
                }
            }
        }
    }

    private void preAssignColors() {
        colorMap = new HashMap<>();
        while(selectStack.size() > 0) {
            Operand toBeColored = selectStack.pop();
            TreeSet<Operand> okColorSet;
            if(type == "Integer") {
                okColorSet = Operand.PhyReg.getOkColorList();
            } else {
                assert type == "Float";
                okColorSet = Operand.FPhyReg.getOkColorList();
            }


            // 待分配节点的邻近节点的颜色不能选
            for(Operand adj: toBeColored.adjOpdSet) {
                Operand opd = adj.getAlias();
                if(opd.needColor(type) && (opd.hasReg() || preColored.contains(opd))) {
                    okColorSet.remove(opd.getPhyReg());
                } else {
                    Operand r = colorMap.get(opd);
                    if(r != null) {
                        okColorSet.remove(r.getPhyReg());
                    }
                }
            }
            if(okColorSet.isEmpty()) {
                spilledNodes.add(toBeColored);
            } else {
                Operand color = okColorSet.pollFirst();

                colorMap.put(toBeColored, color);
            }
        }
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
            HashSet<Operand> live = new LinkedHashSet<>(mcBlock.liveOutSet);
//            System.out.println(mcBlock.getName());
//            System.out.println(mcBlock.liveOutSet.size());
//            System.out.println(mcBlock.getName());
            for(MyNode myNode = mcBlock.getMcLastInstr(); myNode !=
                    mcBlock.getMcInstrs().head; myNode = myNode.getPrev()) {
                McInstr mcInstr = (McInstr) myNode;
//                if(mcInstr instanceof McMove && ((McMove) mcInstr).getDstOp().toString().equals("v12")) {
//                    System.out.println(mcBlock.getName());
//                    System.out.println(mcInstr);
//                    for(Operand li: live) {
//                        System.out.println(li);
//                    }
//                }
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

//        if(mcInstr instanceof McMove && ((McMove) mcInstr).getDstOp().toString().equals("v12")) {
//            System.out.println("----------begin---------");
//            System.out.println(mcInstr);
//            for(Operand li: live){
//                System.out.println(li);
//            }
//            System.out.println("----------end-----------");
//        }

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
//        if(mcInstr instanceof McCall && ((McCall) mcInstr).mcFunction.getName().equals("param16")) {

//            System.out.println(mcInstr);
//            System.out.println(mcInstr.defOperands.size());
//            System.out.println(mcInstr.useOperands.size());
//            for(Operand li: live){
//                System.out.println(li);
//            }

//        }



    }

    private void livenessAnalysis() {

        for(McBlock mcBlock: curMcFunc.getMcBlocks()){
            mcBlock.liveUseSet = new LinkedHashSet<>();
            mcBlock.liveDefSet = new LinkedHashSet<>();
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
            mcBlock.liveInSet = new LinkedHashSet<>(mcBlock.liveUseSet);
            mcBlock.liveOutSet = new LinkedHashSet<>();
        }
        liveInOutAnalysis();
    }

    private void liveInOutAnalysis() {
        boolean changed = true;
        while (changed){
            changed = false;
            for(MyNode iter = curMcFunc.getMcLastBlock(); iter !=
                    curMcFunc.getMcBlocks().head; iter = iter.getPrev()){
//                System.out.println("*************begin*************");

                McBlock mcBlock = (McBlock) iter;
//                System.out.println(mcBlock.getName());
                HashSet<Operand> newLiveOut = new LinkedHashSet<>();
                for(McBlock succMcBlock: mcBlock.getSuccMcBlocks()) {
//                    System.out.println(succMcBlock.getName());
                    for(Operand liveIn : succMcBlock.liveInSet) {
                        if(!mcBlock.liveOutSet.contains(liveIn)) {
                            newLiveOut.add(liveIn);
                            mcBlock.liveOutSet.add(liveIn);
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
//                System.out.println("*************end*************");
            }
        }
    }

    private void addEdge(Operand u, Operand v) {
        Edge edge = new Edge( u, v);
//        if(u.toString().equals("v688") || v.toString().equals("v688")) {
//            System.out.println(edge);
//        }

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

        @Override
        public String toString() {
            return "edge " + u + " "  + v + "\n";
        }
    }
}
