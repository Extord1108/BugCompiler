package frontend.semantic.symbol;

import frontend.semantic.InitVal;
import ir.Value;
import ir.type.Type;

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

    public Object getNumber(){
        return Integer.parseInt("0");
    }

    public String getName(){
        return name;
    }
}
