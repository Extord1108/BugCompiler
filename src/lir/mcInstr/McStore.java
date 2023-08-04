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
}
