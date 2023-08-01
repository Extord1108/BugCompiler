package ir;

import ir.type.ArrayType;
import ir.type.FloatType;
import ir.type.Int32Type;
import ir.type.Type;
import util.MyList;

import java.util.ArrayList;

public class Variable extends Value {

    public Variable(Type type) {
        this.type = type;
    }

    public static class ConstInt extends Variable {
        Integer intVal;

        public ConstInt(int intVal) {
            super(Int32Type.getInstance());
            this.intVal = intVal;
            this.name = ((Integer)intVal).toString();
        }

        public Integer getIntVal() {
            return intVal;
        }

        @Override
        public String toString() {
            return intVal.toString();
        }
    }

    public static class ConstFloat extends Variable {
        Float floatVal;

        public ConstFloat(float floatVal) {
            super(FloatType.getInstance());
            this.floatVal = floatVal;
            this.name = String.format("0x%x", Double.doubleToRawLongBits((floatVal)));
        }

        public Float getFloatVal() {
            return floatVal;
        }

        @Override
        public String toString() {
            String out = String.format("0x%x", Double.doubleToRawLongBits((floatVal)));
            return out;
        }
    }

    public static class VarArray extends Variable {
        private ArrayList<Value> varArray = new ArrayList<>();
        private boolean addZero = true;

        public VarArray(Type type) {
            super(type);
        }

        public void setAddZero(boolean addZero) {
            this.addZero = addZero;
        }

        public void add(Value variable) {
            varArray.add(variable);
        }

        public int getSize() {
            return varArray.size();
        }

        public ArrayList<Value> flatten(){
            ArrayList<Value> flatten = new ArrayList<>();
            for(Value variable: varArray){
                if(variable instanceof VarArray){
                    flatten.addAll(((VarArray) variable).flatten());
                }else{
                    flatten.add(variable);
                }
            }
            return flatten;
        }

        public ArrayList<Value> getvarArray() {
            return varArray;
        }

        public VarArray changeType(ArrayType type) {
            Variable.VarArray ret = new Variable.VarArray(type);
            int count = 0;
            int needSize = type.getSize();
            for (int i = 0; count < needSize && i < this.getSize(); i++) {
                Value variable = varArray.get(i);
                // System.out.println("need " + type.getBasicType());
                if (varArray.get(i) instanceof Variable.VarArray) {// 数组则放到下一层考虑
                    // System.out.println("get " + constant);
                    assert type.getBasicType() instanceof ArrayType;
                    ((Variable.VarArray) variable).setAddZero(addZero);
                    ret.add(((Variable.VarArray) variable).changeType((ArrayType) type.getBasicType()));
                } else {
                    if(!(type.getBasicType() instanceof ArrayType)){//非数组元素直接加入
                        // System.out.println("get " + constant);
                        ret.add(variable);
                        // System.out.println("add " + constant);
                    } else {// 非基本类型则需要拆分
                        // System.out.println("else get " + constant);
                        int size = ((ArrayType) type.getBasicType()).getFattenSize();
                        Variable.VarArray childArray = new Variable.VarArray(type.getBasicType());
                        for (int j = 0; j < size // 要加入子数组的元素个数
                                && i < this.getSize() // 要加入的元素个数不能超过原数组的大小
//                                && (varArray.get(i) instanceof Variable.ConstInt
//                                        || varArray.get(i) instanceof Variable.ConstFloat) // 要加入的元素必须是基本类型
                        ; j++, i++) {
                            // System.out.println("add " + varArray.get(i));
                            childArray.add(varArray.get(i));
                        }
                        if (i < this.getSize())
                            i--;// 如果还有剩余则回退一个
                        else if (i == this.getSize() && childArray.getSize() < size) { // 如果没有剩余但是子数组的元素个数不够
                            for (int j = childArray.getSize(); j < size; j++) {
                                if(!addZero)
                                {
                                    childArray.add(new Undef(type.getContextType()));
                                }
                                else if (type.getContextType() instanceof Int32Type) {
                                    childArray.add(new Variable.ConstInt(0));
                                } else {
                                    assert type.getContextType() instanceof FloatType;
                                    childArray.add(new Variable.ConstFloat(0));
                                }
                                // System.out.println("add 0");
                            }
                        }
                        assert childArray.getSize() == size;
                        ret.add(childArray.changeType((ArrayType) type.getBasicType()));
                    }
                }
                count++;
            }
            // 如果count < needSize则需要补0
            while (count < needSize) {
                if (type.getBasicType() instanceof Int32Type) {
                    if(!addZero)
                    {
                        ret.add(new Undef(type.getContextType()));
                    } else {
                        ret.add(new Variable.ConstInt(0));
                    }
                    // System.out.println("add 0");
                } else if (type.getBasicType() instanceof FloatType) {
                    if(!addZero)
                    {
                        ret.add(new Undef(type.getContextType()));
                    } else {
                        ret.add(new Variable.ConstFloat(0));
                    }
                    // System.out.println("add 0");
                } else if (type.getBasicType() instanceof ArrayType) {
                    int size = ((ArrayType) type.getBasicType()).getFattenSize();
                    Variable.VarArray childArray = new Variable.VarArray(type.getBasicType());
                    for (int j = 0; j < size; j++) {
                        if(!addZero)
                        {
                            childArray.add(new Undef(type.getContextType()));
                        } else if (type.getContextType() instanceof Int32Type) {
                            childArray.add(new Variable.ConstInt(0));
                        } else {
                            assert type.getContextType() instanceof FloatType;
                            childArray.add(new Variable.ConstFloat(0));
                        }
                        // System.out.println("add 0");
                    }
                    ret.add(childArray.changeType((ArrayType) type.getBasicType()));
                }
                count++;
            }
            assert count == needSize;
            return ret;
        }

        @Override
        public String toString() {
            String ret = "[";
            for (int i = 0; i < varArray.size() - 1; i++) {
                ret = ret + varArray.get(i).getType() + " " + varArray.get(i).toString() + ", ";
            }
            if (varArray.size() > 0)
                ret += varArray.get(varArray.size() - 1).getType() + " " + varArray.get(varArray.size() - 1).toString() + "]";
            return ret;
        }
    }

    public static class Undef extends Variable {
        public Undef(Type type){
            super(type);
        }
    }

    public static class ZeroInit extends Variable {

        public ZeroInit(Type type) {
            super(type);
        }

        @Override
        public String toString(){
            return "zeroinitializer";
        }
    }

    //编译期才能进行求值的变量
    public static class VarExp extends Variable{
        private final Value result;

        public VarExp(Value result,Type type){
            super(type);
            this.result = result;
        }

        public Value getResult() {
            return result;
        }
    }
}
