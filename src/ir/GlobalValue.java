package ir;

import frontend.semantic.InitVal;
import ir.type.Type;

public class GlobalValue extends Value{
    public InitVal initVal;
    public GlobalValue(Type type, InitVal initVal){
        this.type = type;
        this.initVal = initVal;
    }

}
