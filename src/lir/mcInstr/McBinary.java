package lir.mcInstr;

import frontend.semantic.OpTree;
import lir.McBlock;
import lir.Operand;

public class McBinary extends McInstr{

    public enum FixType {
        VAR_STACK,
        PARAM_STACK,

    }
    public FixType fixType;
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

    public void setNeedFix(FixType fixType) {
        this.needFix = true;
        this.fixType = fixType;
    }

    public boolean useVFP() {
        return defOperands.get(0).isFloat() || useOperands.get(0).isFloat() || useOperands.get(1).isFloat();
    }

    public int  getOffset(){
        assert this.needFix;
        return ((Operand.Imm)useOperands.get(1)).getIntNumber();
    }

    @Override
    public String toString() {
        if(useVFP()) {
            if(type.equals(BinaryType.Div)) {
                return "vdiv.F32\t" + defOperands.get(0) + ",\t" +
                        useOperands.get(0) + ",\t" + useOperands.get(1);
            }
            return "v" + type + ".F32\t" + defOperands.get(0) + ",\t" +
                    useOperands.get(0) + ",\t" + useOperands.get(1);
        }
        return type + "\t" + defOperands.get(0) + ",\t" + useOperands.get(0) + ",\t" + useOperands.get(1);
    }

    public enum BinaryType {
        Neg("neg"),
        Not("not"),
        Mul("mul","fmul"),
        Div("sdiv","div"),
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
        Or("orr"),
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
