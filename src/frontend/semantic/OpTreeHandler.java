package frontend.semantic;

import java.util.Iterator;

import ir.BasicBlock;
import ir.Value;
import ir.type.FloatType;
import ir.type.Int32Type;
import ir.type.Type;
import ir.Variable;
import ir.instruction.Binary;
import ir.instruction.Unary;

public class OpTreeHandler {
    public static Value evalExp(OpTree opTree, BasicBlock basicBlock, Type defContentType) {
        if (opTree.getType() == OpTree.OpType.binaryType) {
            return evalBinaryExp(opTree, basicBlock, defContentType);
        } else if (opTree.getType() == OpTree.OpType.unaryType) {
            return evalUnaryExp(opTree, basicBlock, defContentType);
        } else if (opTree.getType() == OpTree.OpType.number) {
            if (defContentType instanceof FloatType) {
                return new Variable.ConstFloat(0);
            } else {
                assert defContentType instanceof Int32Type;
                return new Variable.ConstInt(0);
            }
        }
        return null;
    }

    private static Value evalBinaryExp(OpTree opTree, BasicBlock basicBlock, Type defContentType) {
        Iterator<OpTree> it = opTree.getChildren().listIterator();
        Iterator<OpTree.Operator> itOp = opTree.getOperators().listIterator();
        OpTree child = it.next();
        Value first = evalExp(child, basicBlock, defContentType);
        Value second = null;
        while (it.hasNext()) {
            child = it.next();
            second = evalExp(child, basicBlock, defContentType);
            first = new Binary(defContentType, itOp.next(), first, second, basicBlock);
        }
        return first;
    }

    private static Value evalUnaryExp(OpTree opTree, BasicBlock basicBlock, Type defContentType) {
        Iterator<OpTree> it = opTree.getChildren().listIterator();
        Iterator<OpTree.Operator> itOp = opTree.getOperators().listIterator();
        OpTree child = it.next();
        Value val = evalExp(child, basicBlock, defContentType);
        val = new Unary(defContentType, itOp.next(), val, basicBlock);
        return val;
    }

    public static Value evalCond(OpTree opTree){
        return null;
    }
}
