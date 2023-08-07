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


    public boolean useVFP(){
        return useOperands.get(0).isFloat();
    }

    @Override
    public String toString() {
        StringBuilder stb = new StringBuilder();
        if(useVFP()) {
            stb.append("v");
        }
        stb.append("str " + useOperands.get(0) +", [" + useOperands.get(1));
        if(useOperands.size() > 2)
            stb.append(", " + useOperands.get(2));
        stb.append("]");
        return stb.toString();
    }
}
