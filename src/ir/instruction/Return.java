package ir.instruction;

import ir.BasicBlock;
import ir.Value;
import ir.type.Type;
import ir.type.VoidType;

public class Return extends Instr {
    private Value returnValue = null;

    public Return(BasicBlock basicBlock) {
        super(VoidType.getInstance(), basicBlock);
    }

    public Return(Value returnValue, BasicBlock basicBlock) {
        super(returnValue.getType(), basicBlock);
        this.returnValue = returnValue;
        this.addUse(returnValue);
    }

    public Value getReturnValue() {
        return returnValue;
    }

    @Override
    public String toString() {
        if (returnValue == null) {
            return "ret void";
        } else {
            return "ret " + returnValue.getType() + " " + returnValue.getName();
        }
    }
}
