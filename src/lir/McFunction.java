package lir;

import ir.Function;
import lir.mcInstr.McInstr;

import java.util.ArrayList;

public class McFunction {
    private Function irFunction;
    private String name;
    private int stackSize;
    public boolean isMain;
    private ArrayList<McBlock> mcBlocks = new ArrayList<>();


    public ArrayList<Operand> vrList = new ArrayList<>();
    public ArrayList<Operand> svrList = new ArrayList<>();

    public McFunction(Function irFunction){
        this.irFunction = irFunction;
        this.name = irFunction.getName();
    }

    public void addMcBlock(McBlock mcBlock){
        mcBlocks.add(mcBlock);
    }

    public String getName() {
        return name;
    }

    public int getStackSize() {
        return stackSize;
    }

    public void addStackSize(int n){
        this.stackSize = this.stackSize + n;
    }

    @Override
    public String toString() {
        StringBuilder stb = new StringBuilder();
        stb.append("\n\n.global\t").append(this.getName()).append("\n");
        for(int i = 0; i < mcBlocks.size(); i++) {
            McBlock mcBlock = mcBlocks.get(i);
            if(i == 0) {

            }

            for(McInstr mcInstr: mcBlock.getMcInstrs()) {
                stb.append(mcInstr.toString()).append("\n");
            }

            if(i == mcBlocks.size() - 1) {

            }
        }
        return stb.toString();
    }
}
