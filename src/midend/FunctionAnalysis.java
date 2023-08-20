package midend;

import ir.BasicBlock;
import ir.Function;
import ir.GlobalValue;
import ir.Value;
import ir.instruction.Call;
import ir.instruction.Instr;
import ir.type.PointerType;

import java.util.ArrayList;
import java.util.HashMap;

public class FunctionAnalysis extends Pass{
    public FunctionAnalysis(HashMap<String, Function> functions, ArrayList<GlobalValue> globals) {
        super(functions, globals);
    }
    @Override
    public void run(){
        boolean changed = true;
        while(changed){
            changed = false;
            for(Function function : this.functions.values())
            {
                if(function.isExternal()) continue;
                boolean ret = runForFunc(function);
                if(ret != function.canGVN()){
                    changed = true;
                }
                function.setCanGVN(ret);
            }
        }

    }

    public boolean runForFunc(Function function){
        for(Function.Param param : function.getParams()){
            if(param.getType() instanceof PointerType){
                return false;
            }
        }
        for(BasicBlock bb : function.getBasicBlocks()){
            for(Instr instr : bb.getInstrs()){
                if(instr instanceof Call){
                    Call call = (Call) instr;
                    if(call.getFunction()!=function && !call.getFunction().canGVN())
                    return false;
                }
                for(Value value : instr.getUses()){
                    if(value instanceof GlobalValue){
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
