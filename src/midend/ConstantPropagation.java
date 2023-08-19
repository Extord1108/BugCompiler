package midend;

import ir.*;
import ir.instruction.*;
import ir.type.FloatType;
import ir.type.Int32Type;
import ir.type.PointerType;
import ir.type.Type;
import org.antlr.v4.runtime.misc.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;

public class ConstantPropagation extends Pass{

    public ConstantPropagation(HashMap<String, Function> functions, ArrayList<GlobalValue> globals) {
        super(functions, globals);
    }

    private LinkedHashSet<CFGEdge> CFGWorkList = new LinkedHashSet<>();
    private LinkedHashSet<SSAEdge> SSAWorkList = new LinkedHashSet<>();
    private LinkedHashSet<CFGEdge> executedEdge = new LinkedHashSet<>();
    private HashMap<Value,ValueState> valueStateHashMap = new HashMap<>();

    private class ValueState{
        public int state;//0:non-init 1:constant 2:unknown
        public Value value;
        public int constantInt;
        public float constantFloat;
        public Type constantType;
        public ValueState(Value value){
            this.value = value;
            if(value instanceof Variable.ConstInt){
                state = 1;
                constantInt = ((Variable.ConstInt) value).getIntVal();
                constantType = Int32Type.getInstance();
            }else if(value instanceof Variable.ConstFloat) {
                state = 1;
                constantFloat = ((Variable.ConstFloat) value).getFloatVal();
                constantType = FloatType.getInstance();
            }else if(value instanceof Variable.Undef){
                state = 2;
                constantType = value.getType();
            }
            else if(value instanceof Function || value instanceof  BasicBlock || value instanceof Function.Param) {
                state = 2;
            }
        }
    }

    private class CFGEdge{
        public BasicBlock from;
        public BasicBlock to;
        public CFGEdge(BasicBlock from,BasicBlock to){
            this.from = from;
            this.to = to;
        }

        @Override
        public String toString() {
            if (from!=null)
            return from.getName()+"->"+to.getName();
            else return "null->"+to.getName();
        }

        @Override
        public boolean equals(Object obj) {
            if(obj instanceof CFGEdge){
                CFGEdge edge = (CFGEdge) obj;
                if(from == null && edge.from == null){
                    if(to == null && edge.to == null) return true;
                    else if(to != null && edge.to != null) return to.equals(edge.to);
                    else return false;
                }else if(from != null && edge.from != null){
                    if(to == null && edge.to == null) return from.equals(edge.from);
                    else if(to != null && edge.to != null) return from.equals(edge.from) && to.equals(edge.to);
                    else return false;
                }else return false;
            }
            return false;
        }
    }

    private class SSAEdge{
        public Value from;
        public Value to;
        public SSAEdge(Value from,Value to){
            this.from = from;
            this.to = to;
        }

        @Override
        public boolean equals(Object obj) {
            if(obj instanceof SSAEdge){
                SSAEdge edge = (SSAEdge) obj;
                //考虑from和to为null的情况
                if(from == null && edge.from == null){
                    if(to == null && edge.to == null) return true;
                    else if(to != null && edge.to != null) return to.equals(edge.to);
                    else return false;
                }else if(from != null && edge.from != null){
                    if(to == null && edge.to == null) return from.equals(edge.from);
                    else if(to != null && edge.to != null) return from.equals(edge.from) && to.equals(edge.to);
                    else return false;
                }else return false;

            }
            return false;
        }
    }

    @Override
    public void run() {
        for (Function function: functions.values()) {
            if (function.isExternal()) continue;
            runForFunc(function);
            //System.out.println("--------------------------");
            reWrite(function);
        }
    }

    private void init(Function function){
        SSAWorkList.clear();
        CFGWorkList.clear();
        executedEdge.clear();
        BasicBlock entry = function.getBasicBlocks().get(0);
        CFGWorkList.add(new CFGEdge(null,entry));
        for(BasicBlock bb:function.getBasicBlocks()){
            for(Instr instr:bb.getInstrs()){
                if(!valueStateHashMap.containsKey(instr))
                valueStateHashMap.put(instr,new ValueState(instr));
                for(Value value:instr.getUses()){
                    if(!valueStateHashMap.containsKey(value))
                    valueStateHashMap.put(value,new ValueState(value));
                }
            }
        }
    }

    private void runForFunc(Function function) {
        init(function);
        while(CFGWorkList.size() > 0 || SSAWorkList.size() > 0){
            if(CFGWorkList.size() > 0){
                CFGEdge edge = CFGWorkList.iterator().next();
                CFGWorkList.remove(edge);
                //if(edge.from!=null)
                //System.out.println("CFGEdge: "+edge.from.getName()+"->"+edge.to.getName());
                if(!isExecutable(edge))
                {
                    //System.out.println("excuting edge");
                    executedEdge.add(edge);
                    //visit erery phi function assiciated with edge.to
                    for(Instr instr:edge.to.getInstrs()){
                        if(instr instanceof Phi){
                            Phi phi = (Phi) instr;
                            visitOperation(phi);
                        }
                    }
                    boolean flag=true;
                    for(CFGEdge otherEdge:executedEdge){
                        if(!otherEdge.equals(edge) &&otherEdge.to.equals(edge.to)){
                            flag=false;
                            break;
                        }
                    }
                    if(flag){
                        for(Instr instr:edge.to.getInstrs()){
                            if(!(instr instanceof Phi))
                            visitOperation(instr);
                        }
                        if(edge.to.getSuccessors().size()==1){
                            CFGWorkList.add(new CFGEdge(edge.to,edge.to.getSuccessors().get(0)));
                        }
                    }
                }
            }
            if(SSAWorkList.size() > 0){
                SSAEdge edge = SSAWorkList.iterator().next();
                SSAWorkList.remove(edge);
                BasicBlock bb = null;
                if(edge.to instanceof Phi){
                    visitOperation(edge.to);
                }
                else {
                    if(edge.to instanceof Instr)
                        bb = ((Instr) edge.to).getBasicBlock();
                    else
                        bb = ((Instr)edge.to.getUser(0)).getBasicBlock();
                    boolean flag=false;
                    for(CFGEdge otherEdge:executedEdge){
                        if(otherEdge.to.equals(bb)){
                            flag=true;
                            break;
                        }
                    }
                    if(flag) {
                        visitOperation(edge.to);
                    }
                }
            }
        }
    }
    public void visitOperation(Value value){
        if(value instanceof Phi){
            Phi phi = (Phi) value;
            visitPhi(phi);
        }else if(value instanceof Branch){
            Branch branch = (Branch) value;
            visitBranch(branch);
        }else if(value instanceof Instr){
            Instr instr = (Instr) value;
            if(instr instanceof Binary){
                visitBinary((Binary) instr);
            }else if(instr instanceof Unary){
                visitUnary((Unary) instr);
            }
            else if(instr instanceof Icmp || instr instanceof Fcmp){
                visitCmp(instr);
            }else if(instr instanceof Fptosi  || instr instanceof Sitofp || instr instanceof Zext || instr instanceof BitCast) {
                visitFormatTrans(instr);
            }
            else{
                valueStateHashMap.get(value).state=2;
            }
        }else if(value instanceof Variable.ConstFloat || value instanceof Variable.ConstInt || value instanceof Variable.ConstI1){
            valueStateHashMap.get(value).state=1;
            if(value instanceof Variable.ConstFloat)
                valueStateHashMap.get(value).constantFloat = ((Variable.ConstFloat) value).getFloatVal();
            else if(value instanceof Variable.ConstInt)
                valueStateHashMap.get(value).constantInt = ((Variable.ConstInt) value).getIntVal();
            else
                valueStateHashMap.get(value).constantInt = ((Variable.ConstI1) value).getBoolVal()?1:0;
        }else{
            valueStateHashMap.get(value).state=2;
        }
    }

    public void visitPhi(Phi phi){
        //System.out.println("visit phi:"+phi);
        for(int i=0;i<phi.getUses().size();i++){
            BasicBlock bb = phi.getBasicBlock().getPredecessors().get(i);
            boolean flag=false;
            CFGEdge e = new CFGEdge(bb,phi.getBasicBlock());
            for(CFGEdge edge:executedEdge){
                if(edge.equals(e)){
                    flag=true;
                }
            }
            //System.out.println("argument:"+phi.getUse(i).getName()+" state:"+valueStateHashMap.get(phi.getUse(i)).state+" bb:"+bb.getName()+" isexceuted:"+flag);
            if(flag){
                if(valueStateHashMap.get(phi.getUse(i)).state == 0){//未初始化
                    continue;
                }else if(valueStateHashMap.get(phi.getUse(i)).state == 1){
                    if(valueStateHashMap.get(phi).state == 1){
                        if(phi.getType() instanceof Int32Type){
                            if(valueStateHashMap.get(phi).constantInt != valueStateHashMap.get(phi.getUse(i)).constantInt){
                                valueStateHashMap.get(phi).state = 2;
                                for(Used used:phi.getUsedInfo()){
                                    SSAWorkList.add(new SSAEdge(phi,used.getUser()));
                                }
                            }
                        }else {
                            if(valueStateHashMap.get(phi).constantFloat != valueStateHashMap.get(phi.getUses().get(i)).constantFloat){
                                valueStateHashMap.get(phi).state = 2;
                                for(Used used:phi.getUsedInfo()){
                                    SSAWorkList.add(new SSAEdge(phi,used.getUser()));
                                }
                            }
                        }
                    }else if(valueStateHashMap.get(phi).state==0){
                        valueStateHashMap.get(phi).state = 1;
                        valueStateHashMap.get(phi).constantInt = valueStateHashMap.get(phi.getUses().get(i)).constantInt;
                        for(Used used:phi.getUsedInfo()){
                            SSAWorkList.add(new SSAEdge(phi,used.getUser()));
                        }
                    }
                }else{
                    if(valueStateHashMap.get(phi).state <= 1){
                        valueStateHashMap.get(phi).state = 2;
                        for(Used used:phi.getUsedInfo()){
                            SSAWorkList.add(new SSAEdge(phi,used.getUser()));
                        }
                    }
                }
            }
        }
    }

    public void visitBranch(Branch branch){
        int state1 = valueStateHashMap.get(branch).state;
        if(valueStateHashMap.get(branch.getUses().get(0)).state==1){
            if(valueStateHashMap.get(branch.getUses().get(0)).constantInt !=0){
                CFGWorkList.add(new CFGEdge(branch.getBasicBlock(),branch.getThenBlock()));
                valueStateHashMap.get(branch).constantInt = 1;
            }else {
                CFGWorkList.add(new CFGEdge(branch.getBasicBlock(),branch.getElseBlock()));
                valueStateHashMap.get(branch).constantInt = 0;
            }
            state1=1;
        }else {
            CFGWorkList.add(new CFGEdge(branch.getBasicBlock(),branch.getThenBlock()));
            CFGWorkList.add(new CFGEdge(branch.getBasicBlock(),branch.getElseBlock()));
            state1=2;
        }
        valueStateHashMap.get(branch).state = state1;
    }

    public void visitBinary(Binary binary){
        int state1 = valueStateHashMap.get(binary).state;
        if(valueStateHashMap.get(binary.getUses().get(0)).state==1 && valueStateHashMap.get(binary.getUses().get(1)).state==1) {
            state1 = 1;
            if(binary.getType() instanceof FloatType){
                switch (binary.getOp()){
                    case Add:
                        valueStateHashMap.get(binary).constantFloat = valueStateHashMap.get(binary.getUses().get(0)).constantFloat+valueStateHashMap.get(binary.getUses().get(1)).constantFloat;
                        break;
                    case Sub:
                        valueStateHashMap.get(binary).constantFloat = valueStateHashMap.get(binary.getUses().get(0)).constantFloat-valueStateHashMap.get(binary.getUses().get(1)).constantFloat;
                        break;
                    case Mul:
                        valueStateHashMap.get(binary).constantFloat = valueStateHashMap.get(binary.getUses().get(0)).constantFloat*valueStateHashMap.get(binary.getUses().get(1)).constantFloat;
                        break;
                    case Div:
                        valueStateHashMap.get(binary).constantFloat = valueStateHashMap.get(binary.getUses().get(0)).constantFloat/valueStateHashMap.get(binary.getUses().get(1)).constantFloat;
                        break;
                    case Mod:
                        valueStateHashMap.get(binary).constantFloat = valueStateHashMap.get(binary.getUses().get(0)).constantFloat%valueStateHashMap.get(binary.getUses().get(1)).constantFloat;
                        break;
                }
            }else{
                switch (binary.getOp()){
                    case Add:
                        valueStateHashMap.get(binary).constantInt = valueStateHashMap.get(binary.getUses().get(0)).constantInt+valueStateHashMap.get(binary.getUses().get(1)).constantInt;
                        break;
                    case Sub:
                        valueStateHashMap.get(binary).constantInt = valueStateHashMap.get(binary.getUses().get(0)).constantInt-valueStateHashMap.get(binary.getUses().get(1)).constantInt;
                        break;
                    case Mul:
                        valueStateHashMap.get(binary).constantInt = valueStateHashMap.get(binary.getUses().get(0)).constantInt*valueStateHashMap.get(binary.getUses().get(1)).constantInt;
                        break;
                    case Div:
                        valueStateHashMap.get(binary).constantInt = valueStateHashMap.get(binary.getUses().get(0)).constantInt/valueStateHashMap.get(binary.getUses().get(1)).constantInt;
                        break;
                    case Mod:
                        valueStateHashMap.get(binary).constantInt = valueStateHashMap.get(binary.getUses().get(0)).constantInt%valueStateHashMap.get(binary.getUses().get(1)).constantInt;
                        break;
                }
            }
        }
        else{
            state1=2;
        }
        if(valueStateHashMap.get(binary).state != state1){
            for(Used used:binary.getUsedInfo()){
                SSAWorkList.add(new SSAEdge(binary,used.getUser()));
            }
            valueStateHashMap.get(binary).state = state1;
        }
    }

    public void visitUnary(Unary unary){
        int state1 = valueStateHashMap.get(unary).state;
        if(valueStateHashMap.get(unary.getUses().get(0)).state==1){
            state1=1;
            if(unary.getType() instanceof FloatType){
                switch (unary.getOp()){
                    case Neg:
                        valueStateHashMap.get(unary).constantFloat = -valueStateHashMap.get(unary.getUses().get(0)).constantFloat;
                        break;
                    case Not:
                        valueStateHashMap.get(unary).constantFloat = valueStateHashMap.get(unary.getUses().get(0)).constantFloat==0?1:0;
                        break;
                }
            }else{
                switch (unary.getOp()){
                    case Neg:
                        valueStateHashMap.get(unary).constantInt = -valueStateHashMap.get(unary.getUses().get(0)).constantInt;
                        break;
                    case Not:
                        valueStateHashMap.get(unary).constantInt = valueStateHashMap.get(unary.getUses().get(0)).constantInt==0?1:0;
                        break;
                }
            }
        }else{
            state1=2;
        }
        if(valueStateHashMap.get(unary).state != state1){
            for(Used used:unary.getUsedInfo()){
                SSAWorkList.add(new SSAEdge(unary,used.getUser()));
            }
            valueStateHashMap.get(unary).state = state1;
        }
    }

    public void visitCmp(Instr instr){
        int state1 = valueStateHashMap.get(instr).state;
        if(instr instanceof Fcmp){
            Fcmp fcmp = (Fcmp) instr;
            if(valueStateHashMap.get(fcmp.getUses().get(0)).state==1 && valueStateHashMap.get(fcmp.getUses().get(1)).state==1) {
                state1 = 1;
                switch (fcmp.getOp()) {
                    case Eq:
                        valueStateHashMap.get(fcmp).constantInt = valueStateHashMap.get(fcmp.getUses().get(0)).constantFloat == valueStateHashMap.get(fcmp.getUses().get(1)).constantFloat?1:0;
                        break;
                    case Ne:
                        valueStateHashMap.get(fcmp).constantInt = valueStateHashMap.get(fcmp.getUses().get(0)).constantFloat != valueStateHashMap.get(fcmp.getUses().get(1)).constantFloat?1:0;
                        break;
                    case Gt:
                        valueStateHashMap.get(fcmp).constantInt = valueStateHashMap.get(fcmp.getUses().get(0)).constantFloat > valueStateHashMap.get(fcmp.getUses().get(1)).constantFloat?1:0;
                        break;
                    case Ge:
                        valueStateHashMap.get(fcmp).constantInt = valueStateHashMap.get(fcmp.getUses().get(0)).constantFloat >= valueStateHashMap.get(fcmp.getUses().get(1)).constantFloat?1:0;
                        break;
                    case Lt:
                        valueStateHashMap.get(fcmp).constantInt = valueStateHashMap.get(fcmp.getUses().get(0)).constantFloat < valueStateHashMap.get(fcmp.getUses().get(1)).constantFloat?1:0;
                        break;
                    case Le:
                        valueStateHashMap.get(fcmp).constantInt = valueStateHashMap.get(fcmp.getUses().get(0)).constantFloat <= valueStateHashMap.get(fcmp.getUses().get(1)).constantFloat?1:0;
                        break;
                }
            }else{
                state1=2;
            }
        }else if(instr instanceof Icmp){
            Icmp icmp = (Icmp) instr;
            if(valueStateHashMap.get(icmp.getUses().get(0)).state==1 && valueStateHashMap.get(icmp.getUses().get(1)).state==1) {
                state1 = 1;
                switch (icmp.getOp()) {
                    case Eq:
                        valueStateHashMap.get(icmp).constantInt = valueStateHashMap.get(icmp.getUses().get(0)).constantInt == valueStateHashMap.get(icmp.getUses().get(1)).constantInt?1:0;
                        break;
                    case Ne:
                        valueStateHashMap.get(icmp).constantInt = valueStateHashMap.get(icmp.getUses().get(0)).constantInt != valueStateHashMap.get(icmp.getUses().get(1)).constantInt?1:0;
                        break;
                    case Gt:
                        valueStateHashMap.get(icmp).constantInt = valueStateHashMap.get(icmp.getUses().get(0)).constantInt > valueStateHashMap.get(icmp.getUses().get(1)).constantInt?1:0;
                        break;
                    case Ge:
                        valueStateHashMap.get(icmp).constantInt = valueStateHashMap.get(icmp.getUses().get(0)).constantInt >= valueStateHashMap.get(icmp.getUses().get(1)).constantInt?1:0;
                        break;
                    case Lt:
                        valueStateHashMap.get(icmp).constantInt = valueStateHashMap.get(icmp.getUses().get(0)).constantInt < valueStateHashMap.get(icmp.getUses().get(1)).constantInt?1:0;
                        break;
                    case Le:
                        valueStateHashMap.get(icmp).constantInt = valueStateHashMap.get(icmp.getUses().get(0)).constantInt <= valueStateHashMap.get(icmp.getUses().get(1)).constantInt?1:0;
                        break;
                }
            }else{
                state1=2;
            }
        }
        if(valueStateHashMap.get(instr).state != state1){
            for(Used used:instr.getUsedInfo()){
                SSAWorkList.add(new SSAEdge(instr,used.getUser()));
            }
            valueStateHashMap.get(instr).state = state1;
        }
    }

    public void visitFormatTrans(Instr instr){
        int state1 = valueStateHashMap.get(instr).state;
        if(instr instanceof Fptosi){
            Fptosi fptosi = (Fptosi) instr;
            if(valueStateHashMap.get(fptosi.getUses().get(0)).state==1){
                state1 = 1;
                valueStateHashMap.get(fptosi).constantInt = (int)valueStateHashMap.get(fptosi.getUses().get(0)).constantFloat;
            }else{
                state1=2;
            }
        }else if(instr instanceof Sitofp){
            Sitofp sitofp = (Sitofp) instr;
            if(valueStateHashMap.get(sitofp.getUses().get(0)).state==1){
                state1 = 1;
                valueStateHashMap.get(sitofp).constantFloat = valueStateHashMap.get(sitofp.getUses().get(0)).constantInt;
            }else{
                state1=2;
            }
        }else if(instr instanceof Zext){
            Zext zext = (Zext) instr;
            if(valueStateHashMap.get(zext.getUses().get(0)).state==1){
                state1 = 1;
                valueStateHashMap.get(zext).constantInt = valueStateHashMap.get(zext.getUses().get(0)).constantInt;
            }else{
                state1=2;
            }
        }else if(instr instanceof BitCast){
            BitCast bitCast = (BitCast) instr;
            if(valueStateHashMap.get(bitCast.getUses().get(0)).state==1){
                state1 = 1;
                valueStateHashMap.get(bitCast).constantInt = valueStateHashMap.get(bitCast.getUses().get(0)).constantInt;
            }else{
                state1=2;
            }
        }
        if(valueStateHashMap.get(instr).state != state1){
            for(Used used:instr.getUsedInfo()){
                SSAWorkList.add(new SSAEdge(instr,used.getUser()));
            }
            valueStateHashMap.get(instr).state = state1;
        }
    }

    public void reWrite(Function function){
        //删除不在executable的to的基本块
        HashSet<BasicBlock> executable = new HashSet<>();
        for(CFGEdge edge:executedEdge){
            executable.add(edge.to);
        }
        for(BasicBlock bb : function.getBasicBlocks()){
            if(!executable.contains(bb)){
                bb.remove();
            }
        }
        //逆后序遍历重写代码
        ReversePostOrder reversePostOrder = new ReversePostOrder(function);
        for(BasicBlock bb:reversePostOrder.get()){
            for(Instr instr:bb.getInstrs()){
                if(instr instanceof Phi) {
                    Phi phi = (Phi) instr;
                    for(int i=0;i<phi.getUses().size();i++){
                        BasicBlock useBlock = phi.getBasicBlock().getPredecessors().get(i);
                        boolean flag = true;
                        CFGEdge cfgEdge = new CFGEdge(useBlock,phi.getBasicBlock());
                        //System.out.println("query edge:"+cfgEdge);
                        for(CFGEdge edge:executedEdge){
                            //System.out.println(edge);
                            if(edge.equals(cfgEdge)){
                                flag = false;
                                break;
                            }
                        }
                        if(flag){
                            phi.getUses().remove(i);
                        }
                    }
                    if (valueStateHashMap.get(phi).state == 1) {
                        if (phi.getType() instanceof FloatType) {
                            phi.repalceUseofMeto(new Variable.ConstFloat(valueStateHashMap.get(phi).constantFloat));
                        } else {
                            phi.repalceUseofMeto(new Variable.ConstInt(valueStateHashMap.get(phi).constantInt));
                        }
                        phi.remove();
                    }
                    else{
                        if(phi.getUses().size()==1){
                            phi.repalceUseofMeto(phi.getUses().get(0));
                            //System.out.println("phi remove:"+phi.getName());
                            phi.remove();
                        }else if(phi.getUses().size()==0){
                            phi.remove();
                        }
                    }
                }
                else if(valueStateHashMap.get(instr).state==2) continue;
                else{
                    if(instr instanceof Binary){
                        Binary binary = (Binary) instr;
                        if(binary.getType() instanceof FloatType) {
                            binary.repalceUseofMeto(new Variable.ConstFloat(valueStateHashMap.get(binary).constantFloat));
                        }else{
                            binary.repalceUseofMeto(new Variable.ConstInt(valueStateHashMap.get(binary).constantInt));
                        }
                        binary.remove();
                    }else if(instr instanceof Unary){
                        Unary unary = (Unary) instr;
                        if(unary.getType() instanceof FloatType) {
                            unary.repalceUseofMeto(new Variable.ConstFloat(valueStateHashMap.get(unary).constantFloat));
                        }else{
                            unary.repalceUseofMeto(new Variable.ConstInt(valueStateHashMap.get(unary).constantInt));
                        }
                        unary.remove();
                    }else if(instr instanceof Fcmp || instr instanceof Icmp){
                        instr.repalceUseofMeto(new Variable.ConstInt(valueStateHashMap.get(instr).constantInt));
                        instr.remove();
                    }else if(instr instanceof Branch){
                        Branch br = (Branch) instr;
                        if(br.getCond()!=null){
                            if(valueStateHashMap.get(br).state==1){
                                if(valueStateHashMap.get(br).constantInt!=0){
                                    Jump jump =  new Jump(br.getThenBlock());
                                    jump.setBasicBlock(br.getBasicBlock());
                                    br.getBasicBlock().getInstrs().insertBefore(br,jump);
                                }else{
                                    Jump jump =  new Jump(br.getElseBlock());
                                    jump.setBasicBlock(br.getBasicBlock());
                                    br.getBasicBlock().getInstrs().insertBefore(br,jump);
                                }
                                br.remove();
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isExecutable(CFGEdge edge) {
        for (CFGEdge cfgEdge : executedEdge) {
            if (cfgEdge.equals(edge)) {
                return true;
            }
        }
        return false;
    }
}
