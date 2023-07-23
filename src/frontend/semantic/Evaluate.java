package frontend.semantic;

import java.util.Iterator;

import org.antlr.v4.runtime.misc.ObjectEqualityComparator;

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
        return opTree.getValue();
    }

    public static Object evalConstBinaryExp(OpTree opTree) {
        Iterator<OpTree> it = opTree.getChildren().listIterator();
        Iterator<OpTree.Operator> itOp = opTree.getOperators().listIterator();
        evalConstExp(it.next());
        String value = it.next().getValue();
        while (it.hasNext()) {
            OpTree child = it.next();
            evalConstExp(child);
            value = binaryExpCalculator(value, child.getValue(), itOp.next()).toString();
        }
        opTree.setValue(value);
        return 0;
    }

    public static Object evalConstUnaryExp(OpTree opTree) {
        Iterator<OpTree> it = opTree.getChildren().listIterator();
        Iterator<OpTree.Operator> itOp = opTree.getOperators().listIterator();
        evalConstExp(it.next());
        String value = it.next().getValue();
        value = unaryExpCalculator(value, itOp.next()).toString();
        opTree.setValue(value);
        return 0;
    }

    private static Object binaryExpCalculator(Object left, Object right, OpTree.Operator op) {
        if (left.toString().contains(".") || left.toString().contains(".")) {
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
        if (num.toString().contains(".")) {
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
}
