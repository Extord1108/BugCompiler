package lir.mcInstr;

import lir.McBlock;
import lir.McFunction;

public class Pop extends McInstr{
    McFunction savedRegsMf;
    public Pop(McFunction savedRegsMf, McBlock mcBlock) {
        super(mcBlock);
        this.savedRegsMf = savedRegsMf;
    }
}
