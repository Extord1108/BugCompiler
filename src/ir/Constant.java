package ir;

import ir.type.ArrayType;
import ir.type.FloatType;
import ir.type.Int32Type;
import ir.type.Type;

import java.util.ArrayList;

public class Constant extends Value{

    public Constant(Type type){
        this.type = type;
    }

    public static class ConstInt extends Constant{
        int intVal;
        public ConstInt(int intVal){
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

    public static class ConstFloat extends Constant{
        float floatVal;
        public ConstFloat(float floatVal){
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

    public static class ConstArray extends Constant{
        private ArrayList<Constant> constArray = new ArrayList<>();
        public ConstArray(Type type){
            super(type);
        }

        public void add(Constant constant){
            constArray.add(constant);
        }

        public int getSize(){
            return constArray.size();
        }

        public ArrayList<Constant> getConstArray() {
            return constArray;
        }

        public ConstArray changeType(ArrayType type){
            Constant.ConstArray ret = new Constant.ConstArray(type);
            int count = 0;
            int needSize = type.getSize();
            while (count < needSize && count < this.getSize()){

            }
            return ret;
        }

//        public Constant getConst(ArrayList<Integer> dims){
//            int index = 0;
//            ArrayType type = (ArrayType) this.type;
//            for(int i = 0; i < dims.size() - 1; i++){
//                index += dims.get(i) * ((ArrayType)type.getBasicType()).getFattenSize();
//            }
//            index += dims.get(dims.size() - 1);
//            assert index < constArray.size();
//            return constArray.get(index);
//        }

        @Override
        public String toString() {
            String ret = "[";
            for(int i = 0; i < constArray.size() - 1; i++){
                ret = ret + constArray.get(i).toString() + ", ";
            }
            ret += constArray.get(constArray.size() - 1).toString() + "]";
            return ret;
        }
    }
}
