package frontend.semantic;

import java.util.ArrayList;

public class OpTree {
    private final ArrayList<OpTree> children;
    private final ArrayList<Operator> operators;
    private final OpTree parent;
    private final OpType type;

    public OpTree(ArrayList<OpTree> children,ArrayList<Operator> operators,OpTree parent,OpType type){
        this.children = children;
        this.operators = operators;
        this.parent = parent;
        this.type = type;
    }
    public void addChild(OpTree child){
        children.add(child);
    }

    public void removeLast() {
        var last = children.get(children.size() - 1);
        children.remove(last);
    }

    public void appendOp(Operator op){
        operators.add(op);
    }

    public ArrayList<Operator> getOperators(){
        return operators;
    }

    public ArrayList<OpTree> getChildren(){
        return children;
    }

    public OpTree getParent(){
        return parent;
    }

    public enum Operator{
        Neg,
        Not,
        Mul,
        Div,
        Mod,
        Add,
        Sub,
        Lt,
        Gt,
        Le,
        Ge,
        Eq,
        Ne,
        And,
        Or,
        CastInt, // 隐式转换
        CastFloat,
    }

    public enum OpType{
        unaryType,
        binaryType
    }
}
