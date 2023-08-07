package lir.mcInstr;

import lir.McBlock;

public class McJump extends McInstr{
    McBlock target;
    public McJump(McBlock target, McBlock mcBlock) {
        super(mcBlock);
        this.target = target;
    }

    @Override
    public String toString() {
        return "b\t" + target.getName();
    }
}
