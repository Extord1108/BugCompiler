package midend;

import ir.*;
import ir.instruction.*;
import ir.type.FloatType;
import ir.type.Type;
import org.antlr.v4.runtime.misc.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class ConstantPropagation extends Pass{

    public ConstantPropagation(HashMap<String, Function> functions, ArrayList<GlobalValue> globals) {
        super(functions, globals);
    }

    private HashSet<CFGEdge> CFGWorkList = new HashSet<>();
    private HashSet<SSAEdge> SSAWorkList = new HashSet<>();
    private HashSet<CFGEdge> executedEdge = new HashSet<>();
    private HashMap<Value,ValueState> valueStateHashMap = new HashMap<>();

    private class ValueState{
        public int state;//0:non-init 1:constant 2:unknown
        public Value value;
        public int constantInt;
        public float constantFloat;
        public boolean constantBool;
        public Type constantType;
        public ValueState(Value value){
            this.value = value;
            this.state = 0;
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
        public boolean equals(Object obj) {
            if(obj instanceof CFGEdge){
                CFGEdge edge = (CFGEdge) obj;
                return edge.from.equals(from) && edge.to.equals(to);
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
                return edge.from.equals(from) && edge.to.equals(to);
            }
            return false;
        }
    }

    @Override
    public void run() {
        for (Function function: functions.values()) {
            if (function.isExternal()) continue;
            runForFunc(function);
            reWrite(function);
        }
    }

    private void init(Function function){
        SSAWorkList.clear();
        CFGWorkList.clear();
        executedEdge.clear();
        BasicBlock entry = function.getBasicBlocks().get(0);
        for(BasicBlock succ:entry.getSuccessors())
        {
            CFGWorkList.add(new CFGEdge(entry,succ));
        }
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
                if(!executedEdge.contains(edge))
                {
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
                        if(otherEdge.to.equals(edge.to)){
                            flag=false;
                            break;
                        }
                    }
                    if(flag){
                        for(Instr instr:edge.to.getInstrs()){
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
            }else{
                valueStateHashMap.get(value).state=2;
            }
        }else if(value instanceof Variable.ConstFloat || value instanceof Variable.ConstInt){
            valueStateHashMap.get(value).state=1;
            if(value instanceof Variable.ConstFloat)
                valueStateHashMap.get(value).constantFloat = ((Variable.ConstFloat) value).getFloatVal();
            else
                valueStateHashMap.get(value).constantInt = ((Variable.ConstInt) value).getIntVal();
        }else{
            valueStateHashMap.get(value).state=2;
        }
    }

    public void visitPhi(Phi phi){
        for(int i=0;i<phi.getUses().size();i++){
            BasicBlock bb = phi.getBasicBlock().getSuccessors().get(i);
            boolean flag=false;
            for(CFGEdge edge:executedEdge){
                if(edge.from.equals(bb) && edge.to.equals(phi.getBasicBlock())){
                    flag=true;
                }
            }
            if(flag){
                if(valueStateHashMap.get(phi).state<valueStateHashMap.get(phi.getUses().get(i)).state){
                    for(Used used:phi.getUsedInfo()){
                        SSAWorkList.add(new SSAEdge(phi,used.getUser()));
                    }
                    valueStateHashMap.get(phi).state = valueStateHashMap.get(phi.getUses().get(i)).state;
                }
            }
        }
    }

    public void visitBranch(Branch branch){
        if(valueStateHashMap.get(branch.getUses().get(0)).state==1){
            if(valueStateHashMap.get(branch.getUses().get(0)).constantBool){
                CFGWorkList.add(new CFGEdge(branch.getBasicBlock(),branch.getBasicBlock().getSuccessors().get(0)));
            }else {
                CFGWorkList.add(new CFGEdge(branch.getBasicBlock(),branch.getBasicBlock().getSuccessors().get(1)));
            }
            valueStateHashMap.get(branch).state=1;
        }else {
            CFGWorkList.add(new CFGEdge(branch.getBasicBlock(),branch.getBasicBlock().getSuccessors().get(0)));
            CFGWorkList.add(new CFGEdge(branch.getBasicBlock(),branch.getBasicBlock().getSuccessors().get(1)));
            valueStateHashMap.get(branch).state=2;
        }
    }

    public void visitBinary(Binary binary){
        if(valueStateHashMap.get(binary.getUses().get(0)).state==1 && valueStateHashMap.get(binary.getUses().get(1)).state==1) {
            valueStateHashMap.get(binary).state = 1;
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
            valueStateHashMap.get(binary).state=2;
        }
    }

    public void visitUnary(Unary unary){
        if(valueStateHashMap.get(unary.getUses().get(0)).state==1){
            valueStateHashMap.get(unary).state=1;
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
            valueStateHashMap.get(unary).state=2;
        }
    }

    public void visitCmp(Instr instr){
        if(instr instanceof Fcmp){
            Fcmp fcmp = (Fcmp) instr;
            if(valueStateHashMap.get(fcmp.getUses().get(0)).state==1 && valueStateHashMap.get(fcmp.getUses().get(1)).state==1) {
                valueStateHashMap.get(fcmp).state = 1;
                switch (fcmp.getOp()) {
                    case Eq:
                        valueStateHashMap.get(fcmp).constantBool = valueStateHashMap.get(fcmp.getUses().get(0)).constantFloat == valueStateHashMap.get(fcmp.getUses().get(1)).constantFloat;
                        break;
                    case Ne:
                        valueStateHashMap.get(fcmp).constantBool = valueStateHashMap.get(fcmp.getUses().get(0)).constantFloat != valueStateHashMap.get(fcmp.getUses().get(1)).constantFloat;
                        break;
                    case Gt:
                        valueStateHashMap.get(fcmp).constantBool = valueStateHashMap.get(fcmp.getUses().get(0)).constantFloat > valueStateHashMap.get(fcmp.getUses().get(1)).constantFloat;
                        break;
                    case Ge:
                        valueStateHashMap.get(fcmp).constantBool = valueStateHashMap.get(fcmp.getUses().get(0)).constantFloat >= valueStateHashMap.get(fcmp.getUses().get(1)).constantFloat;
                        break;
                    case Lt:
                        valueStateHashMap.get(fcmp).constantBool = valueStateHashMap.get(fcmp.getUses().get(0)).constantFloat < valueStateHashMap.get(fcmp.getUses().get(1)).constantFloat;
                        break;
                    case Le:
                        valueStateHashMap.get(fcmp).constantBool = valueStateHashMap.get(fcmp.getUses().get(0)).constantFloat <= valueStateHashMap.get(fcmp.getUses().get(1)).constantFloat;
                        break;
                }
            }else{
                valueStateHashMap.get(fcmp).state=2;
            }
        }else if(instr instanceof Icmp){
            Icmp icmp = (Icmp) instr;
            if(valueStateHashMap.get(icmp.getUses().get(0)).state==1 && valueStateHashMap.get(icmp.getUses().get(1)).state==1) {
                valueStateHashMap.get(icmp).state = 1;
                switch (icmp.getOp()) {
                    case Eq:
                        valueStateHashMap.get(icmp).constantBool = valueStateHashMap.get(icmp.getUses().get(0)).constantInt == valueStateHashMap.get(icmp.getUses().get(1)).constantInt;
                        break;
                    case Ne:
                        valueStateHashMap.get(icmp).constantBool = valueStateHashMap.get(icmp.getUses().get(0)).constantInt != valueStateHashMap.get(icmp.getUses().get(1)).constantInt;
                        break;
                    case Gt:
                        valueStateHashMap.get(icmp).constantBool = valueStateHashMap.get(icmp.getUses().get(0)).constantInt > valueStateHashMap.get(icmp.getUses().get(1)).constantInt;
                        break;
                    case Ge:
                        valueStateHashMap.get(icmp).constantBool = valueStateHashMap.get(icmp.getUses().get(0)).constantInt >= valueStateHashMap.get(icmp.getUses().get(1)).constantInt;
                        break;
                    case Lt:
                        valueStateHashMap.get(icmp).constantBool = valueStateHashMap.get(icmp.getUses().get(0)).constantInt < valueStateHashMap.get(icmp.getUses().get(1)).constantInt;
                        break;
                    case Le:
                        valueStateHashMap.get(icmp).constantBool = valueStateHashMap.get(icmp.getUses().get(0)).constantInt <= valueStateHashMap.get(icmp.getUses().get(1)).constantInt;
                        break;
                }
            }else{
                valueStateHashMap.get(icmp).state=2;
            }
        }
    }

    public void reWrite(Function function){
        //逆后序遍历重写代码
        ReversePostOrder reversePostOrder = new ReversePostOrder(function);
        for(BasicBlock bb:reversePostOrder.get()){
            for(Instr instr:bb.getInstrs()){
                if(valueStateHashMap.get(instr).state==2) continue;
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
                        instr.repalceUseofMeto(new Variable.ConstInt(valueStateHashMap.get(instr).constantBool?1:0));
                        instr.remove();
                    }else if(instr instanceof Branch){
                        Branch br = (Branch) instr;
                        if(br.getCond()!=null){
                            if(valueStateHashMap.get(br).state==1){
                                if(valueStateHashMap.get(br.getCond()).constantBool){
                                    new Jump(br.getThenBlock(),br.getBasicBlock());
                                }else{
                                    new Jump(br.getElseBlock(),br.getBasicBlock());
                                }
                                br.remove();
                            }
                        }
                    }else if(instr instanceof Phi) {
                        Phi phi = (Phi) instr;
                        if (valueStateHashMap.get(phi).state == 1) {
                            if (phi.getType() instanceof FloatType) {
                                phi.repalceUseofMeto(new Variable.ConstFloat(valueStateHashMap.get(phi).constantFloat));
                            } else {
                                phi.repalceUseofMeto(new Variable.ConstInt(valueStateHashMap.get(phi).constantInt));
                            }
                            phi.remove();
                        }
                    }
                }
            }
        }
    }
}
