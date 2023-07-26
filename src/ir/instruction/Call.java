package ir.instruction;

import ir.BasicBlock;
import ir.Function;
import ir.Value;
import ir.type.Type;
import ir.type.VoidType;

import java.util.ArrayList;

public class Call extends Instr{

    Function function;
    ArrayList<Value> params;

    public Call(Function function, ArrayList<Value> params, BasicBlock basicBlock) {
        super(function.getType(), basicBlock);
        this.function = function;
        this.params = params;
    }

    @Override
    public String toString() {
        String prefix = "";
        String returnType = "void";
        if(!(type instanceof VoidType)){
            prefix = getName() + " = ";
            returnType = type.toString();
        }
        String params = "";
        for(int i = 0; i < this.params.size(); i++){
            params += this.params.get(i).getType() + " " + this.params.get(i).getName();
            if(i != this.params.size() - 1)
                params += ", ";
        }
        return prefix + "call " + returnType + " @" + function.getName() + "(" + params + ")";
    }
}
