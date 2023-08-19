package ir.instruction;

import ir.BasicBlock;
import ir.Function;
import ir.Value;
import ir.type.Type;
import ir.type.VoidType;

import java.util.ArrayList;

public class Call extends Instr{

    public Call(Function function, ArrayList<Value> params, BasicBlock basicBlock) {
        super(function.getType(), basicBlock);
        this.addUse(function);
        for(Value param: params){
            this.addUse(param);
        }
    }

    public ArrayList<Value> getParams() {
        ArrayList<Value> params = new ArrayList<>();
        for(int i = 1; i < this.getUses().size(); i++){
            params.add(this.getUse(i));
        }
        return params;
    }

    @Override
    public Instr clone(BasicBlock bb){
        ArrayList<Value> params = new ArrayList<>();
        for(Value param: this.getParams()){
            params.add(param.getClone());
        }
        this.cloneInstr = new Call(this.getFunction(), params, bb);
        return this.cloneInstr;
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
        for(int i = 0; i < this.getParams().size(); i++){
            params += this.getParams().get(i).getType() + " " + this.getParams().get(i).getName();
            if(i != this.getParams().size() - 1)
                params += ", ";
        }
        // if(getFunction().getName().equals("starttime") || getFunction().getName().equals("stoptime"))
        // {
        //     return prefix + "call " + returnType + " @_sysy_" + getFunction().getName() + "( i32 0 )";
        // }
        return prefix + "call " + returnType + " @" + getFunction().getName() + "(" + params + ")";
    }

    public Function getFunction() {
        return (Function) getUse(0);
    }
}
