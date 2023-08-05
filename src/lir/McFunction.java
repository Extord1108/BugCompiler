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

    public ArrayList<Operand.PhyReg> usedPhyRegs = new ArrayList<>();
    public ArrayList<Operand.FPhyReg> usedFPhyRegs = new ArrayList<>();

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
        stb.append("\n\n.global\t").append(this.getName()).append("\n").append(this.getName()).append(":\n\n");
        for(int i = 0; i < mcBlocks.size(); i++) {

            McBlock mcBlock = mcBlocks.get(i);
            stb.append(this.getName()).append("_").append(mcBlock.getName()).append(":\n");

            if(i == 0) {
                stb.append("\t").append("push\t{");
                stb.append("}\n");
                stb.append("\t").append("vpush\t{");
                stb.append("}\n");
                if(stackSize != 0)
                    stb.append("\t").append("sub\tsp,\tsp,\t#").append(stackSize).append("\n");
            }

            for(McInstr mcInstr: mcBlock.getMcInstrs()) {
                stb.append("\t").append(mcInstr.toString()).append("\n");
            }



            if(i == mcBlocks.size() - 1) {
                if(stackSize != 0)
                    stb.append("\t").append("add\tsp,\tsp,\t#").append(stackSize).append("\n");
                stb.append("\t").append("vpop\t{");
                stb.append("}\n");
                stb.append("\t").append("pop\t{");
                stb.append("}\n");

            }

            stb.append("\n");
        }
        return stb.toString();
    }
}
