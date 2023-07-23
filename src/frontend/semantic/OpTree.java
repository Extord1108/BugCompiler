package frontend.semantic;

import java.util.ArrayList;

public class OpTree {
    private final ArrayList<OpTree> children;
    private final ArrayList<Operator> operators;
    private OpTree parent;
    private final OpType type;
    private ConstNumber number;

    public OpTree(ArrayList<OpTree> children,ArrayList<Operator> operators,OpTree parent,OpType type){
        this.children = children;
        this.operators = operators;
        this.parent = parent;
        this.type = type;
    }

    public OpTree(OpTree parent, OpType type){
        children = null;
        operators = null;
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

    public void setParent(OpTree parent) {
        this.parent = parent;
    }

    public Object getNumber(){
        if(number == null)
            System.err.println("Current OpTree Type is " + type.toString());
        return number.getNumber();
    }

    public void setNumber(Object number){
        if(this.number == null)
            this.number = new ConstNumber(number);
        else
            this.number.setNumber(number);
    }

    public ConstNumber.NumberType getNumberType(){
        return number.getType();
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
        binaryType,
        number
    }
}
