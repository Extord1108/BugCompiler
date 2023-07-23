package frontend.semantic.symbol;

import frontend.semantic.InitVal;
import ir.type.Type;

public class Symbol {
    private final String name;
    private final Type type;
    private final boolean isConst;
    private final InitVal initVal;

    public Symbol(String name,Type type,boolean isConst,InitVal initVal){
        this.name = name;
        this.type = type;
        this.isConst = isConst;
        this.initVal = initVal;
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
