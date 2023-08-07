package lir.mcInstr;

import frontend.semantic.OpTree;
import lir.McBlock;
import lir.Operand;

public class McBinary extends McInstr{

    public BinaryType type;
    public boolean needFix = false;
    public McBinary(BinaryType binaryType, Operand dst, Operand src1, Operand src2, McBlock mcBlock) {
        super(mcBlock);
        this.type = binaryType;
        defOperands.add(dst);
        useOperands.add(src1);
        useOperands.add(src2);
    }

    public McBinary(BinaryType binaryType, Operand dst, Operand src1, Operand src2, MCShift shift, McBlock mcBlock) {
        super(mcBlock);
        this.type = binaryType;
        defOperands.add(dst);
        useOperands.add(src1);
        useOperands.add(src2);
    }

    public void setNeedFix(boolean needFix) {
        this.needFix = needFix;
    }

    public boolean useVFP() {
        return defOperands.get(0).isFloat() || useOperands.get(0).isFloat() || useOperands.get(1).isFloat();
    }

    @Override
    public String toString() {
        if(useVFP()) {
            return "v" + type + ".F32\t" + defOperands.get(0) + ",\t" + useOperands.get(0) + ",\t" + useOperands.get(1);
        }
        return type + "\t" + defOperands.get(0) + ",\t" + useOperands.get(0) + ",\t" + useOperands.get(1);
    }

    public enum BinaryType {
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
        Rsb("rsb")
        ;

        private final String name;
        private final String fname;

        private BinaryType(String name){
            this.name = name;
            this.fname = "";
        }

        private BinaryType(String name, String fname){
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
}
