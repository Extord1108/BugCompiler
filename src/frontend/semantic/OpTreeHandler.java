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

    public static Value evalCond(OpTree opTree, BasicBlock trueBlock, BasicBlock falseBlock, BasicBlock basicBlock) {
        if (opTree.getType() == OpTree.OpType.condType) {
            return evalBinaryCond(opTree, trueBlock, falseBlock, basicBlock);
        } else if (opTree.getType() == OpTree.OpType.binaryType) {
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

    private static Value evalBinaryCond(OpTree opTree, BasicBlock trueBlock, BasicBlock falseBlock,
            BasicBlock basicBlock) {
        Iterator<OpTree> it = opTree.getChildren().listIterator();
        Iterator<OpTree.Operator> itOp = opTree.getOperators().listIterator();
        OpTree child = it.next();
        Value first = evalCond(child, trueBlock, falseBlock, basicBlock);
        Value second = null;
        while (it.hasNext()) {
            OpTree.Operator op = itOp.next();
            if (op == OpTree.Operator.And) {
                child = it.next();
                first = Visitor.Instance.turnTo(first, Int1Type.getInstance());
                second = evalCond(child, trueBlock, falseBlock, basicBlock);
                BasicBlock newTrueBlock = new BasicBlock(basicBlock.getFunction());
                newTrueBlock.addInstr(new Branch(second, trueBlock, falseBlock, newTrueBlock));
                first = new Branch(first, newTrueBlock, falseBlock, basicBlock);
            } else if (op == OpTree.Operator.Or) {
                child = it.next();
                second = evalCond(child, trueBlock, falseBlock, basicBlock);
                BasicBlock newFalseBlock = new BasicBlock(basicBlock.getFunction());
                newFalseBlock.addInstr(new Branch(second, trueBlock, falseBlock, newFalseBlock));
                first = new Branch(first, trueBlock, newFalseBlock, basicBlock);
            } else if (op == OpTree.Operator.Eq || op == OpTree.Operator.Ne) {
                child = it.next();

            } else {
                child = it.next();
                second = evalExp(child, basicBlock);
                if (first.getType() instanceof FloatType || second.getType() instanceof FloatType) {
                    Visitor.Instance.turnTo(second, FloatType.getInstance());
                    Visitor.Instance.turnTo(second, FloatType.getInstance());
                    first = new Fcmp(first, second, op, basicBlock);
                } else {
                    assert first.getType() instanceof Int32Type && second.getType() instanceof Int32Type;
                    first = new Icmp(first, second, op, basicBlock);
                }
            }
        }
        return first;
    }
}
