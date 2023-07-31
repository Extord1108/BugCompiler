package frontend.semantic;

import ir.Value;

import java.util.ArrayList;

public class OpTree {
    private final ArrayList<OpTree> children;
    private final ArrayList<Operator> operators;
    private OpTree parent;
    private final OpType type;
    private Value value;
    private ConstNumber number;
    private boolean needPointer;

    public OpTree(ArrayList<OpTree> children, ArrayList<Operator> operators, OpTree parent, OpType type) {
        this.children = children;
        this.operators = operators;
        this.parent = parent;
        this.type = type;
    }

    public OpTree(OpTree parent, OpType type) {
        if(type == OpType.funcType || type == OpType.arrayType){
            children = new ArrayList<>();
            operators = new ArrayList<>();
        }else{
            children = null;
            operators = null;
        }
        this.parent = parent;
        this.type = type;
    }

    public boolean getNeedPointer(){
        return this.needPointer;
    }
    public void setNeedPointer(boolean needPointer) {
        this.needPointer = needPointer;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    public Value getValue() {
        return value;
    }

    public void addChild(OpTree child) {
        children.add(child);
    }

    public OpTree getLast() {
        return children.get(children.size() - 1);
    }

    public void removeLast() {
        var last = children.get(children.size() - 1);
        children.remove(last);
    }

    public void appendOp(Operator op) {
        operators.add(op);
    }

    public ArrayList<Operator> getOperators() {
        return operators;
    }

    public ArrayList<OpTree> getChildren() {
        return children;
    }

    public OpTree getParent() {
        return parent;
    }

    public void setParent(OpTree parent) {
        this.parent = parent;
    }

    public Object getNumber() {
        if (number == null)
            System.err.println("Current OpTree Type is " + type.toString());
        return number.getNumber();
    }

    public void setNumber(Object number) {
        if (this.number == null)
            this.number = new ConstNumber(number);
        else
            this.number.setNumber(number);
    }

    public ConstNumber.NumberType getNumberType() {
        return number.getType();
    }

    public OpType getType() {
        return type;
    }

    public enum Operator {
        Neg("neg"),
        Not("not"),
        Mul("mul","fmul"),
        Div("sdiv","fdiv"),
        Mod("srem","frem"),
        Add("add","fadd"),
        Sub("sub","fsub"),
        Lt("slt","olt"),
        Gt("sgt","ogt"),
        Le("sle","ole"),
        Ge("sge","oge"),
        Eq("eq","oeq"),
        Ne("ne","one"),
        And("and"),
        Or("or"),
        CastInt("castint"), // 隐式转换
        CastFloat("casefloat"),
        ;

        private final String name;
        private final String fname;

        private Operator(String name){
            this.name = name;
            this.fname = "";
        }

        private Operator(String name, String fname){
            this.name = name;
            this.fname = fname;
        }


        public String getName(){
            return name;
        }

        public String getfName() {
            return fname;
        }

        @Override
        public String toString(){
            return name;
        }
    }

    public enum OpType {
        unaryType,
        binaryType,
        condType,
        number,
        valueType,
        loadType,
        funcType,
        arrayType

    }
}
