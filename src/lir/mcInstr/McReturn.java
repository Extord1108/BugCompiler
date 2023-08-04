package lir.mcInstr;

import lir.McBlock;
import lir.Operand;

public class McReturn extends McInstr{


    public McReturn(Operand ret, McBlock mcBlock) {
        super(mcBlock);
        useOperands.add(ret);
    }

    public McReturn(McBlock mcBlock){
        super(mcBlock);
    }
}
