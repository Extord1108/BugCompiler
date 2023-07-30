package lir;

import ir.Function;

public class McFunction {
    private Function irFunction;
    private String name;

    public McFunction(Function irFunction){
        this.irFunction = irFunction;
        this.name = irFunction.getName();
    }
}
