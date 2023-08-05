package lir.mcInstr;

import lir.McBlock;

public class McBranch extends McInstr{

    Cond cond;
    McBlock target;

    public McBranch(Cond cond, McBlock target, McBlock mcBlock) {
        super(mcBlock);
        this.cond = cond;
        this.target = target;
    }
}
