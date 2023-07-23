package frontend.semantic.symbol;

import ir.type.Type;

public class Symbol {
    private final String name;
    private final Type type;
    private final boolean isConst;

    public Symbol(String name,Type type,boolean isConst){
        this.name = name;
        this.type = type;
        this.isConst = isConst;
    }

    public boolean isConst(){
        return this.isConst;
    }

    public String getName(){
        return name;
    }
}
