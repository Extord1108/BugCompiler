package lir.mcInstr;

import lir.McBlock;
import lir.McFunction;

public class Push extends McInstr{
    McFunction savedRegsMf;
    public Push(McFunction savedRegsMf, McBlock mcBlock) {
        super(mcBlock);
        this.savedRegsMf = savedRegsMf;
    }
}
