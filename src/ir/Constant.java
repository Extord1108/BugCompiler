package ir;

import ir.type.ArrayType;
import ir.type.FloatType;
import ir.type.Int32Type;
import ir.type.Type;

import java.util.ArrayList;

public class Constant extends Value {

    public Constant(Type type) {
        this.type = type;
    }

    public static class ConstInt extends Constant {
        int intVal;

        public ConstInt(int intVal) {
            super(Int32Type.getInstance());
            this.intVal = intVal;
        }

        public int getIntVal() {
            return intVal;
        }

        @Override
        public String toString() {
            return type + " " + intVal;
        }
    }

    public static class ConstFloat extends Constant {
        float floatVal;

        public ConstFloat(float floatVal) {
            super(FloatType.getInstance());
            this.floatVal = floatVal;
        }

        public float getFloatVal() {
            return floatVal;
        }

        @Override
        public String toString() {
            return type + " " + floatVal;
        }
    }

    public static class ConstArray extends Constant {
        private ArrayList<Constant> constArray = new ArrayList<>();

        public ConstArray(Type type) {
            super(type);
        }

        public void add(Constant constant) {
            constArray.add(constant);
        }

        public int getSize() {
            return constArray.size();
        }

        public ArrayList<Constant> getConstArray() {
            return constArray;
        }

        public ConstArray changeType(ArrayType type) {
            Constant.ConstArray ret = new Constant.ConstArray(type);
            int count = 0;
            int needSize = type.getSize();
            for (int i = 0; count < needSize && i < this.getSize(); i++) {
                Constant constant = constArray.get(i);
                // System.out.println("need " + type.getBasicType());
                if (constArray.get(i) instanceof Constant.ConstArray) {// 数组则放到下一层考虑
                    // System.out.println("get " + constant);
                    assert type.getBasicType() instanceof ArrayType;
                    ret.add(((Constant.ConstArray) constant).changeType((ArrayType) type.getBasicType()));
                } else {
                    if (type.getBasicType() instanceof Int32Type || type.getBasicType() instanceof FloatType) {// 基本类型直接加入
                        // System.out.println("get " + constant);
                        ret.add(constant);
                        // System.out.println("add " + constant);
                    } else {// 非基本类型则需要拆分
                        // System.out.println("else get " + constant);
                        int size = ((ArrayType) type.getBasicType()).getFattenSize();
                        Constant.ConstArray childArray = new Constant.ConstArray(type.getBasicType());
                        for (int j = 0; j < size // 要加入子数组的元素个数
                                && i < this.getSize() // 要加入的元素个数不能超过原数组的大小
                                && (constArray.get(i) instanceof Constant.ConstInt
                                        || constArray.get(i) instanceof Constant.ConstFloat) // 要加入的元素必须是基本类型
                        ; j++, i++) {
                            // System.out.println("add " + constArray.get(i));
                            childArray.add(constArray.get(i));
                        }
                        if (i < this.getSize())
                            i--;// 如果还有剩余则回退一个
                        else if (i == this.getSize() && childArray.getSize() < size) { // 如果没有剩余但是子数组的元素个数不够
                            for (int j = childArray.getSize(); j < size; j++) {
                                if (type.getContextType() instanceof Int32Type) {
                                    childArray.add(new Constant.ConstInt(0));
                                } else {
                                    assert type.getContextType() instanceof FloatType;
                                    childArray.add(new Constant.ConstFloat(0));
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
                    ret.add(new Constant.ConstInt(0));
                    // System.out.println("add 0");
                } else if (type.getBasicType() instanceof FloatType) {
                    ret.add(new Constant.ConstFloat(0));
                    // System.out.println("add 0");
                } else if (type.getBasicType() instanceof ArrayType) {
                    int size = ((ArrayType) type.getBasicType()).getFattenSize();
                    Constant.ConstArray childArray = new Constant.ConstArray(type.getBasicType());
                    for (int j = 0; j < size; j++) {
                        if (type.getContextType() instanceof Int32Type) {
                            childArray.add(new Constant.ConstInt(0));
                        } else {
                            assert type.getContextType() instanceof FloatType;
                            childArray.add(new Constant.ConstFloat(0));
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

        // public Constant getConst(ArrayList<Integer> dims){
        // int index = 0;
        // ArrayType type = (ArrayType) this.type;
        // for(int i = 0; i < dims.size() - 1; i++){
        // index += dims.get(i) * ((ArrayType)type.getBasicType()).getFattenSize();
        // }
        // index += dims.get(dims.size() - 1);
        // assert index < constArray.size();
        // return constArray.get(index);
        // }

        @Override
        public String toString() {
            String ret = "[";
            for (int i = 0; i < constArray.size() - 1; i++) {
                ret = ret + constArray.get(i).toString() + ", ";
            }
            if (constArray.size() > 0)
                ret += constArray.get(constArray.size() - 1).toString() + "]";
            return ret;
        }
    }
}
