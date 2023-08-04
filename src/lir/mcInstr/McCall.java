package lir.mcInstr;

import lir.McBlock;
import lir.McFunction;

public class McCall extends McInstr{
    public McFunction mcFunction;
    public McCall(McFunction mcFunction, McBlock mcBlock) {
        super(mcBlock);
        this.mcFunction = mcFunction;
    }
}
