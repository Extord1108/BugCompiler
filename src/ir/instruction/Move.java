package ir.instruction;

import ir.BasicBlock;
import ir.Value;
import ir.Variable;
import ir.type.Int32Type;
import ir.type.Type;

public class Move extends Instr{
    private Value from, to;
    public Move(Type type, Value from, Value to, BasicBlock basicBlock) {
        //super(type, basicBlock);
        this.type = type;
        this.from = from;
        this.to = to;
        this.basicBlock = basicBlock;
    }

    public Value getFrom() {
        return from;
    }

    public Value getTo() {
        return to;
    }

    @Override
    public String toString() {
        return "move " + this.type + " " + this.to.getName() + "<- " + this.from.getName();
//        if(this.type instanceof Int32Type){
//            return this.to.getName() + " = " + "add " + this.type.toString() + " " + this.from.getName() + ", 0";
//        }else{
//            return this.to.getName() + " = " + "fadd " + this.type.toString() + " " + this.from.getName()+ ", 0x0";
//        }
    }
}
