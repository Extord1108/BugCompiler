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
                genPush(stb);
                if(stackSize != 0)
                    stb.append("\t").append("sub\tsp,\tsp,\t#").append(stackSize).append("\n");
            }

            for(McInstr mcInstr: mcBlock.getMcInstrs()) {
                stb.append("\t").append(mcInstr.toString()).append("\n");
            }



            if(i == mcBlocks.size() - 1) {
                if(stackSize != 0)
                    stb.append("\t").append("add\tsp,\tsp,\t#").append(stackSize).append("\n");
            }
        }
        return stb.toString();
    }

    private void genPush(StringBuilder stb) {
        StringBuilder popRegs = new StringBuilder();
        StringBuilder popFRegs = new StringBuilder();
        int idx = 0;
        for(Operand reg: usedPhyRegs) {
            idx ++;
            if(((Operand.PhyReg)reg).getIdx() > 3 && !reg.equals(Operand.PhyReg.getPhyReg("sp"))){
                popRegs.append(reg);
                if(idx < usedPhyRegs.size()) {
                    popRegs.append(", ");
                }
            }
        }
        idx = 0;
        for(Operand reg: usedFPhyRegs) {
            idx ++;
            if(((Operand.FPhyReg)reg).getIdx() > 15) {
                popFRegs.append(reg);
                if(idx < usedFPhyRegs.size()) {
                    popFRegs.append(", ");
                }
            }
        }
        if(!popFRegs.toString().equals("")) {
            stb.append("\tvpop\t{"+ popFRegs.toString()+"}\n");
        }
        if(!popRegs.toString().equals("")) {
            stb.append("\tpop\t{"+ popRegs.toString()+ "}\n");
        }
    }
}
