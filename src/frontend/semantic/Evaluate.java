package frontend.semantic;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * 用于在编译期进行求值操作
 */
public class Evaluate {

    public static Object evalConstExp(OpTree opTree) {

        if (opTree.getType() == OpTree.OpType.binaryType) {
            evalConstBinaryExp(opTree);
        }
        if (opTree.getType() == OpTree.OpType.unaryType) {
            evalConstUnaryExp(opTree);
        }
        return opTree.getNumber();
    }

    public static Object evalConstBinaryExp(OpTree opTree) {
        Iterator<OpTree> it = opTree.getChildren().listIterator();
        Iterator<OpTree.Operator> itOp = opTree.getOperators().listIterator();
        OpTree child = it.next();
        evalConstExp(child);
        Object value = child.getNumber();
        while (it.hasNext()) {
            child = it.next();
            evalConstExp(child);
            value = binaryExpCalculator(value, child.getNumber(), itOp.next());
        }
        opTree.setNumber(value);

        return 0;
    }

    public static Object evalConstUnaryExp(OpTree opTree) {
        Iterator<OpTree> it = opTree.getChildren().listIterator();
        Iterator<OpTree.Operator> itOp = opTree.getOperators().listIterator();
        OpTree child = it.next();
        evalConstExp(child);
        Object value = child.getNumber();
        value = unaryExpCalculator(value, itOp.next());
        opTree.setNumber(value);
        return 0;
    }

    private static Object binaryExpCalculator(Object left, Object right, OpTree.Operator op) {
        if ((left instanceof Float) || (right instanceof Float)) {
            float lnum = left instanceof Integer ? (float) ((int) left) : (float) left;
            float rnum = right instanceof Integer ? (float) ((int) right) : (float) right;
            return switch (op) {
                case Add -> lnum + rnum;
                case Sub -> lnum - rnum;
                case Mul -> lnum * rnum;
                case Div -> lnum / rnum;
                case Mod -> lnum % rnum;
                default -> throw new AssertionError("Bad Binary Operator");
            };
        } else {
            assert left instanceof Integer && right instanceof Integer;
            int lnum = (int) left;
            int rnum = (int) right;
            return switch (op) {
                case Add -> lnum + rnum;
                case Sub -> lnum - rnum;
                case Mul -> lnum * rnum;
                case Div -> lnum / rnum;
                case Mod -> lnum % rnum;
                default -> throw new AssertionError("Bad Binary Operator");
            };
        }
    }

    private static Object unaryExpCalculator(Object num, OpTree.Operator op) {
        if (num instanceof Float) {
            float fnum = num instanceof Integer ? (float) ((int) num) : (float) num;
            return switch (op) {
                case Add -> fnum;
                case Neg -> -fnum;
                case Not -> fnum == 0 ? 1 : 0;
                default -> throw new AssertionError("Bad Unary Operator");
            };
        } else {
            assert num instanceof Integer;
            int inum = (int) num;
            return switch (op) {
                case Add -> inum;
                case Neg -> -inum;
                case Not -> inum == 0 ? 1 : 0;
                default -> throw new AssertionError("Bad Unary Operator");
            };
        }
    }

    public static void main(String[] args) {
        OpTree opTree = new OpTree(new ArrayList<OpTree>(), new ArrayList<OpTree.Operator>(), null,
                OpTree.OpType.binaryType);

        OpTree child1 = new OpTree(new ArrayList<OpTree>(), new ArrayList<OpTree.Operator>(), opTree,
                OpTree.OpType.unaryType);
        OpTree child1_1 = new OpTree(child1, OpTree.OpType.number);
        child1_1.setNumber((int) 1);
        child1.appendOp(OpTree.Operator.Neg);
        child1.addChild(child1_1);

        OpTree child2 = new OpTree(new ArrayList<OpTree>(), new ArrayList<OpTree.Operator>(), opTree,
                OpTree.OpType.binaryType);
        OpTree child2_1 = new OpTree(child2, OpTree.OpType.number);
        child2_1.setNumber((int) 2);
        OpTree child2_2 = new OpTree(child2, OpTree.OpType.number);
        child2_2.setNumber((int) 3);
        child2.appendOp(OpTree.Operator.Add);
        child2.addChild(child2_1);
        child2.addChild(child2_2);

        opTree.appendOp(OpTree.Operator.Mul);
        opTree.addChild(child1);
        opTree.addChild(child2);

        Object value = evalConstExp(opTree);
        System.out.println(value instanceof Integer);
    }
}
