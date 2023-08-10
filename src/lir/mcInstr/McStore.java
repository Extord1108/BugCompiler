package lir.mcInstr;

import lir.McBlock;
import lir.Operand;

public class McStore extends McInstr{

    public McStore(Operand data, Operand addr, Operand offset, McBlock mcBlock) {
        super(mcBlock);
        useOperands.add(data);
        useOperands.add(addr);
        useOperands.add(offset);
    }

    public McStore(Operand data, Operand addr, McBlock mcBlock) {
        super(mcBlock);
        useOperands.add(data);
        useOperands.add(addr);
    }

    public McStore(Operand data, Operand addr, Operand offset, McBlock mcBlock, boolean insert) {
        super(mcBlock, insert);
        useOperands.add(data);
        useOperands.add(addr);
        useOperands.add(offset);
    }

    public Operand getAddr() {
        return useOperands.get(1);
    }

    public Operand getOffset() {
        if(useOperands.size() < 3)
            return null;
        return useOperands.get(2);
    }

    public Operand getData() {
        return useOperands.get(0);
    }


    public boolean useVFP(){
        return useOperands.get(0).isFloat();
    }

    @Override
    public String toString() {
        StringBuilder stb = new StringBuilder();
        if(!useVFP()) {
            stb.append("str" + "\t" + useOperands.get(0) + ",\t" + "[" + useOperands.get(1));
            if(useOperands.size() > 2)
                stb.append(",\t" + useOperands.get(2));
            stb.append("]");
        } else {
            stb.append("vstr.F32" + "\t" + useOperands.get(0) + ",\t" + "[" + useOperands.get(1));
            if(useOperands.size() > 2)
                stb.append(",\t" + useOperands.get(2));
            stb.append("]");
        }
        return stb.toString();
    }
}
