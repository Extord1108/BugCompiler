package frontend.semantic;

import java.util.Iterator;

import frontend.parser.Visitor;
import ir.BasicBlock;
import ir.Value;
import ir.type.FloatType;
import ir.type.Int1Type;
import ir.type.Int32Type;
import ir.type.Type;
import ir.Variable;
import ir.instruction.Binary;
import ir.instruction.Branch;
import ir.instruction.Fcmp;
import ir.instruction.Icmp;
import ir.instruction.Unary;

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
        } else if (opTree.getType() == OpTree.OpType.valueType) {
            return opTree.getValue();
        }
        return null;
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
            if (first.getType() instanceof Int32Type && second.getType() instanceof Int32Type) {
                currentType = Int32Type.getInstance();
            } else {
                assert (first.getType() instanceof FloatType || second.getType() instanceof FloatType);
                currentType = FloatType.getInstance();
            }
            first = new Binary(currentType, itOp.next(), first, second, basicBlock);
        }
        return first;
    }

    private static Value evalUnaryExp(OpTree opTree, BasicBlock basicBlock) {
        Iterator<OpTree> it = opTree.getChildren().listIterator();
        Iterator<OpTree.Operator> itOp = opTree.getOperators().listIterator();
        OpTree child = it.next();
        Value val = evalExp(child, basicBlock);
        val = new Unary(val.getType(), itOp.next(), val, basicBlock);
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
                BasicBlock newTrueBlock = new BasicBlock(Visitor.Instance.getCurBasicBlock().getFunction());
                first = Visitor.Instance.turnTo(first, Int1Type.getInstance());
                new Branch(first, newTrueBlock, falseBlock, Visitor.Instance.getCurBasicBlock());
                Visitor.Instance.setCurBasicBlock(newTrueBlock);
                first = evalCond(child, null, null);
            } else if (op == OpTree.Operator.Or) {// or时，左右都为Int1
                if(it.hasNext()) {
                    BasicBlock newFalseBlock = new BasicBlock(Visitor.Instance.getCurBasicBlock().getFunction());
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
