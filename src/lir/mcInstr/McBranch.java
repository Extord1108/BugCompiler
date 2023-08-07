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

    @Override
    public String toString() {
        StringBuilder stb = new StringBuilder();
        stb.append("b").append(cond).append("\t" + target.getName());
        return stb.toString();
    }
}
