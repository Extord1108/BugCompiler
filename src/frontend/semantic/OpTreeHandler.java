package frontend.semantic;

import java.util.ArrayList;
import java.util.Iterator;

import frontend.parser.Visitor;
import ir.BasicBlock;
import ir.Function;
import ir.Value;
import ir.instruction.*;
import ir.type.*;
import ir.Variable;

public class OpTreeHandler {

    private static Value turnToInt1(Value value, BasicBlock curBasicBlock) {
        if (value.getType().equals(Int1Type.getInstance())) {
            return value;
        } else if (value.getType().equals(Int32Type.getInstance())) {
            return new Icmp(value, new Variable.ConstInt(0), OpTree.Operator.Ne, curBasicBlock);
        } else {
            return new Fcmp(value, new Variable.ConstFloat(0), OpTree.Operator.Ne, curBasicBlock);
        }

    }

    public static Value evalExp(OpTree opTree, BasicBlock basicBlock) {
        if (opTree.getType() == OpTree.OpType.binaryType) {
            return evalBinaryExp(opTree, basicBlock);
        } else if (opTree.getType() == OpTree.OpType.unaryType) {
            return evalUnaryExp(opTree, basicBlock);
        } else if (opTree.getType() == OpTree.OpType.number) {
            if (opTree.getNumberType() == ConstNumber.NumberType.Float) {
                return new Variable.ConstFloat((float) opTree.getNumber());
            } else {
                assert opTree.getNumberType() == ConstNumber.NumberType.INT;
                return new Variable.ConstInt((int) opTree.getNumber());
            }
        } else if (opTree.getType() == OpTree.OpType.loadType){
            return new Load(opTree.getValue(), basicBlock);
        } else if(opTree.getType() == OpTree.OpType.arrayType) {
            return evalArray(opTree, basicBlock);
        } else if(opTree.getType() == OpTree.OpType.funcType){
            ArrayList<Value> params = new ArrayList<>();
            Function function = (Function) opTree.getValue();
            Iterator<OpTree> it = opTree.getChildren().listIterator();
            int i = 0;
            while (it.hasNext()){
                Value value = evalExp(it.next(), basicBlock);
                Function.Param param = function.getParams().get(i);
                if (param.getType() instanceof Int32Type || param.getType() instanceof FloatType) {
                    value = Visitor.Instance.turnTo(value, param.getType());
                }
                params.add(value);
                i ++;
            }
            return new Call(function, params, basicBlock);
        }
        else if (opTree.getType() == OpTree.OpType.valueType) {
            return opTree.getValue();
        }
        return null;
    }

    public static Value evalArray(OpTree opTree, BasicBlock basicBlock){
        ArrayList<Value> idxList = new ArrayList<>();
        Value pointer = opTree.getValue();
        Type basicType = pointer.getType().getBasicType();
        boolean first = true;
        Iterator<OpTree> it = opTree.getChildren().listIterator();
        while (it.hasNext()){
            Value offset = OpTreeHandler.evalExp(it.next(), basicBlock);
            offset = Visitor.Instance.turnTo(offset, Int32Type.getInstance());
            if(first)
            {
                first = false;
                if(basicType instanceof PointerType){
                    basicType = basicType.getBasicType();
                    pointer = new Load(pointer, basicBlock);
                }else{
                    assert basicType instanceof ArrayType;
                    basicType = basicType.getBasicType();
                    idxList.add(Visitor.Instance.getCONST_0());
                }
                idxList.add(offset);
            }else{
                basicType = basicType.getBasicType();
                idxList.add(offset);
            }
        }
        // 防止a[10]取a的情况出现，没有idxList
        if(idxList.size() != 0)
            pointer = new GetElementPtr(basicType, pointer, idxList, basicBlock);
        if(opTree.getNeedPointer()){
            return pointer;
        }
        Value value;
        if(basicType instanceof ArrayType){
            idxList =  new ArrayList<>();
            idxList.add(Visitor.Instance.getCONST_0());
            idxList.add(Visitor.Instance.getCONST_0());
            value = new GetElementPtr(basicType.getBasicType(), pointer, idxList, basicBlock);
        }else {
            value =  new Load(pointer, basicBlock);
        }
        return value;
    }

    private static Value evalBinaryExp(OpTree opTree, BasicBlock basicBlock) {
        Iterator<OpTree> it = opTree.getChildren().listIterator();
        Iterator<OpTree.Operator> itOp = opTree.getOperators().listIterator();
        OpTree child = it.next();
        Value first = evalExp(child, basicBlock);
        Value second = null;
        while (it.hasNext()) {
            child = it.next();
            second = evalExp(child, basicBlock);
            Type currentType = null;
            if(first.getType() instanceof FloatType || second.getType() instanceof FloatType) {
                currentType = FloatType.getInstance();
                first = Visitor.Instance.turnTo(first,FloatType.getInstance());
                second = Visitor.Instance.turnTo(second,FloatType.getInstance());
            } else {
                currentType = Int32Type.getInstance();
                first = Visitor.Instance.turnTo(first,Int32Type.getInstance());
                second = Visitor.Instance.turnTo(second,Int32Type.getInstance());
            }
            first = new Binary(currentType, itOp.next(), first, second, basicBlock);
        }
        return first;
    }

    private static Value evalUnaryExp(OpTree opTree, BasicBlock basicBlock) {
        Iterator<OpTree> it = opTree.getChildren().listIterator();
        OpTree.Operator op = opTree.getOperators().get(0);
        OpTree child = it.next();
        Value val = evalExp(child, basicBlock);
        if(op ==OpTree.Operator.Not) {
            if(val.getType() instanceof FloatType) {
                val = new Fcmp(val, Visitor.Instance.getCONST_0f(), OpTree.Operator.Eq, basicBlock);
            } else {
                val = Visitor.Instance.turnTo(val, Int32Type.getInstance());
                val = new Icmp(val, Visitor.Instance.getCONST_0(), OpTree.Operator.Eq, basicBlock);
            }
//            val = new Unary(Int1Type.getInstance(), op, val, basicBlock);
        }
        else{
            if(val.getType() instanceof Int1Type)
                val = Visitor.Instance.turnTo(val, Int32Type.getInstance());
            val = new Unary(val.getType(), op, val, basicBlock);
        }

        return val;
    }

    public static Value evalCond(OpTree opTree, BasicBlock trueBlock, BasicBlock falseBlock) {
        if (opTree.getType() == OpTree.OpType.condType) {
            return evalBinaryCond(opTree, trueBlock, falseBlock);
        } else {
            return evalExp(opTree, Visitor.Instance.getCurBasicBlock());
        }
    }

    private static Value evalBinaryCond(OpTree opTree, BasicBlock trueBlock, BasicBlock falseBlock) {
        Iterator<OpTree> it = opTree.getChildren().listIterator();
        Iterator<OpTree.Operator> itOp = opTree.getOperators().listIterator();
        OpTree child;
        Value first = null;
        if(!(it.hasNext() && opTree.getOperators().get(0) ==OpTree.Operator.Or)){
            child = it.next();
            first = evalCond(child, trueBlock, falseBlock);;
        }
        Value second = null;
        while (it.hasNext()) {
            child = it.next();
            OpTree.Operator op = OpTree.Operator.Or;
            if(itOp.hasNext())
                op = itOp.next();


            if (op == OpTree.Operator.And) { // and时，左右都为Int1
                BasicBlock newTrueBlock = new BasicBlock(Visitor.Instance.getCurBasicBlock().getFunction(),Visitor.Instance.getCurLoop());
                first = Visitor.Instance.turnTo(first, Int1Type.getInstance());
                new Branch(first, newTrueBlock, falseBlock, Visitor.Instance.getCurBasicBlock());
                Visitor.Instance.setCurBasicBlock(newTrueBlock);
                first = evalCond(child, null, null);
            } else if (op == OpTree.Operator.Or) {// or时，左右都为Int1
                if(it.hasNext()) {
                    BasicBlock newFalseBlock = new BasicBlock(Visitor.Instance.getCurBasicBlock().getFunction(),Visitor.Instance.getCurLoop());
                    first = evalCond(child, trueBlock, newFalseBlock);
                    first = Visitor.Instance.turnTo(first, Int1Type.getInstance());
                    new Branch(first, trueBlock, newFalseBlock, Visitor.Instance.getCurBasicBlock());
                    Visitor.Instance.setCurBasicBlock(newFalseBlock);
                }
                else
                {
                    first = evalCond(child, trueBlock, falseBlock);
                }
            } else {// 比较运算时，根据左边和右边的类型进行相应转换
                second = evalCond(child, trueBlock, falseBlock);
                if (first.getType() instanceof FloatType || second.getType() instanceof FloatType) {// 其中有一个float则都为float
                    first = Visitor.Instance.turnTo(first, FloatType.getInstance());
                    second = Visitor.Instance.turnTo(second, FloatType.getInstance());
                    first = new Fcmp(first, second, op, Visitor.Instance.getCurBasicBlock());
                } else if (first.getType() instanceof Int32Type || second.getType() instanceof Int32Type) {// 其中有一个int则都为int
                    first = Visitor.Instance.turnTo(first, Int32Type.getInstance());
                    second = Visitor.Instance.turnTo(second, Int32Type.getInstance());
                    first = new Icmp(first, second, op, Visitor.Instance.getCurBasicBlock());
                } else {
                    assert op == OpTree.Operator.Eq || op == OpTree.Operator.Ne;// 只有ne和eq的两边才可能为int1
                    assert first.getType() instanceof Int1Type && second.getType() instanceof Int1Type;
                    first = new Icmp(first, second, op, Visitor.Instance.getCurBasicBlock());
                }
            }
        }
        return first;
    }
}
