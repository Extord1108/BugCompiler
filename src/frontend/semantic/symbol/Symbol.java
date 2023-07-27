package frontend.semantic.symbol;

import frontend.semantic.InitVal;
import ir.Value;
import ir.Variable;
import ir.type.FloatType;
import ir.type.Int32Type;
import ir.type.Type;

import java.awt.geom.Arc2D;

public class Symbol {
    private final String name;
    private final Type type;
    private final boolean isConst;
    private final InitVal initVal;
    private final Value pointer;

    public Symbol(String name,Type type,boolean isConst,InitVal initVal,Value pointer){
        this.name = name;
        this.type = type;
        this.isConst = isConst;
        this.initVal = initVal;
        this.pointer = pointer;
    }

    public boolean isConst(){
        return this.isConst;
    }

    public Value getValue() {
        return pointer;
    }

    public Object getNumber(){
        if(type instanceof Int32Type){
            return ((Variable.ConstInt)(initVal.getValue())).getIntVal();
        }else{
            assert type instanceof FloatType;
            return  ((Variable.ConstFloat)(initVal.getValue())).getFloatVal();
        }

    }

    public String getName(){
        return name;
    }
}
