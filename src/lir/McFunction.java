package lir;

import ir.Function;
import lir.mcInstr.McInstr;
import util.MyList;

import java.util.ArrayList;
import java.util.TreeSet;

public class McFunction {
    private Function irFunction;
    private String name;
    private int stackSize;
    public boolean isMain;
    public boolean useLr = false;
    private MyList<McBlock> mcBlocks = new MyList<McBlock>();


    public ArrayList<Operand> vrList = new ArrayList<>();
    public ArrayList<Operand> svrList = new ArrayList<>();

    public TreeSet<Operand.PhyReg> usedPhyRegs = new TreeSet<>();
    public TreeSet<Operand.FPhyReg> usedFPhyRegs = new TreeSet<>();

    public McFunction(Function irFunction){
        this.irFunction = irFunction;
        this.name = irFunction.getName();
    }

    public void addMcBlock(McBlock mcBlock){
        mcBlocks.insertTail(mcBlock);
    }

    public MyList<McBlock> getMcBlocks() {
        return mcBlocks;
    }

    public void setUseLr() {
        useLr = true;
        usedPhyRegs.add(Operand.PhyReg.getPhyReg("lr"));
    }
    public McBlock getMcLastBlock() {
        return mcBlocks.getLast();
    }

    public String getName() {
        return name;
    }

    public ArrayList<Operand> getSvrList() {
        return svrList;
    }

    public ArrayList<Operand> getVrList() {
        return vrList;
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
            stb.append(mcBlock.getName()).append(":\n");

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
