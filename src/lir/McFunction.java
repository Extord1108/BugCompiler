package lir;

import ir.Function;

import java.util.ArrayList;

public class McFunction {
    private Function irFunction;
    private String name;
    private int stackSize;
    public boolean isMain;


    public ArrayList<Operand> vrList = new ArrayList<>();
    public ArrayList<Operand> svrList = new ArrayList<>();

    public McFunction(Function irFunction){
        this.irFunction = irFunction;
        this.name = irFunction.getName();
    }

    public int getStackSize() {
        return stackSize;
    }

    public void addStackSize(int n){
        this.stackSize = this.stackSize + n;
    }
}
