package lir;

import ir.GlobalValue;

public class Operand {

    public enum OperandType{
        imm,    //  立即数
        virtual,    //  虚拟寄存器
    }

    String pre;
    boolean isFloat = false;

    public boolean isImm(){
        return this instanceof Imm;
    }

    public static class VirtualReg extends Operand{
        private static int vrCount = 0;
        private int value;
        public VirtualReg(boolean isFloat, McFunction mcFunction){
            this.value = vrCount++;
            this.isFloat = isFloat;
            if(isFloat)
                mcFunction.svrList.add(this);
            else
                mcFunction.vrList.add(this);
        }
    }

    public static class Global extends Operand {
        private GlobalValue globalValue;
        public Global(GlobalValue globalValue){
            this.globalValue = globalValue;
        }
    }


    public static class Imm extends Operand{
        private int intNumber;
        private float floatNumber;

        public Imm( int intNumber){
            this.isFloat = false;
            this.intNumber = intNumber;
            this.pre = "#";
        }

        public Imm(float floatNumber){
            this.isFloat = true;
            this.floatNumber = floatNumber;
            this.pre = "#";
        }

        public int getIntNumber() {
            return intNumber;
        }

        public float getFloatNumber() {
            return floatNumber;
        }
    }
}
