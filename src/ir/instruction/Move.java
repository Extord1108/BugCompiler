package ir.instruction;

import ir.BasicBlock;
import ir.Value;
import ir.Variable;
import ir.type.Type;

public class Move extends Instr{
    private Value from, to;
    public Move(Type type, Value from, Value to, BasicBlock basicBlock) {
        //super(type, basicBlock);
        this.type = type;
        this.from = from;
        this.to = to;
    }

    @Override
    public String toString() {
        return "move " + this.type + " " + this.from.getName() + ", " + this.to.getName();
    }
}
