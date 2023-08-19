package ir.instruction;

import ir.BasicBlock;
import ir.Value;
import ir.type.Type;
import ir.type.VoidType;

public class Return extends Instr {

    public Return(BasicBlock basicBlock) {
        super(VoidType.getInstance(), basicBlock);
    }

    public Return(Value returnValue, BasicBlock basicBlock) {
        super(returnValue.getType(), basicBlock);
        this.addUse(returnValue);
    }

    public Value getReturnValue() {
        if(this.getUses().isEmpty())
            return null;
        else
            return this.getUse(0);
    }

    @Override
    public Instr clone(BasicBlock bb){
        if(this.getUses().isEmpty())
            this.cloneInstr = new Return(bb);
        else
            this.cloneInstr = new Return(this.getUse(0).getClone(), bb);
        return this.cloneInstr;
    }

    @Override
    public String toString() {
        if (this.getUses().isEmpty()) {
            return "ret void";
        } else {
            return "ret " + this.getUse(0).getType() + " " + this.getUse(0).getName();
        }
    }
}
