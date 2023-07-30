package ir;

import ir.instruction.Instr;
import util.MyNode;

// 保存使用相关信息，新建的主要原因是为了解决一个MyNode职能有一个前驱和后继的问题
public class Used extends MyNode {
    private Instr user;
    private Value use;

    public Used(Instr user, Value use){
        this.user = user;
        this.use = use;
    }
}
