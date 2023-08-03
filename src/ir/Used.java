package ir;

import ir.instruction.Instr;
import util.MyNode;

// 保存使用相关信息，新建的主要原因是为了解决一个MyNode职能有一个前驱和后继的问题
public class Used extends MyNode {
    private Instr user;//使用了value的指令
    private Value use;//value本身

    public Used(Instr user, Value use){
        this.user = user;
        this.use = use;
    }

    public Instr getUser() {
        return user;
    }

    public Value getValue() {
        return use;
    }
}
