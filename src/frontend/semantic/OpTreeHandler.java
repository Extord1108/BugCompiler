package frontend.semantic;

import java.util.Iterator;

import frontend.semantic.OpTree.OpType;
import ir.BasicBlock;
import ir.Value;
import ir.type.FloatType;
import ir.type.Int32Type;
import ir.type.Type;
import ir.Variable;
import ir.instruction.Binary;
import ir.instruction.Unary;

public class OpTreeHandler {
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

    public static Value evalCond(OpTree opTree) {
        // if (opTree.getType() == OpTree.OpType.binaryType) {
        // return evalBinaryCond(opTree, basicBlock);
        // } else if (opTree.getType() == OpTree.OpType.unaryType) {
        // return evalUnaryCond(opTree, basicBlock);
        // }
        return null;
    }

    // private static Value evalBinaryCond(OpTree opTree, BasicBlock basicBlock) {
    // Iterator<OpTree> it = opTree.getChildren().listIterator();
    // Iterator<OpTree.Operator> itOp = opTree.getOperators().listIterator();
    // OpTree child = it.next();
    // Value first = evalExp(child, basicBlock);
    // Value second = null;
    // while (it.hasNext()) {
    // child = it.next();
    // second = evalExp(child, basicBlock);
    // Type currentType = null;
    // if (first.getType() instanceof Int32Type && second.getType() instanceof
    // Int32Type) {
    // currentType = Int32Type.getInstance();
    // } else {
    // assert (first.getType() instanceof FloatType || second.getType() instanceof
    // FloatType);
    // currentType = FloatType.getInstance();
    // }
    // first = new Binary(currentType, itOp.next(), first, second, basicBlock);
    // }
    // return first;
    // }

    // private static Value evalUnaryCond(OpTree opTree, BasicBlock basicBlock) {
    // Iterator<OpTree> it = opTree.getChildren().listIterator();
    // Iterator<OpTree.Operator> itOp = opTree.getOperators().listIterator();
    // OpTree child = it.next();
    // Value val = evalExp(child, basicBlock);
    // val = new Unary(val.getType(), itOp.next(), val, basicBlock);
    // return val;
    // }
}
