package lir;

import ir.Function;
import ir.GlobalValue;
import lir.mcInstr.McMove;
import manager.Manager;

import java.lang.reflect.Field;
import java.util.*;

public class Operand {

    String pre;
    boolean isFloat = false;

    public HashSet<McMove> moveList;

    public Set<Operand> adjOpdSet;

    public int degree = 0;

    private Operand alias;

    private boolean recentSpill = false;

    double weight = 0.0;

    public Operand phyReg = null;

    private int stackPos = -1;

    public void setStackPos(int stackPos) {
        this.stackPos = stackPos;
    }

    public int getStackPos() {
        return stackPos;
    }

    public boolean hasReg() {
        return phyReg != null;
    }

    public Operand getPhyReg() {
        return phyReg;
    }

    public double getWeight() {
        return weight;
    }

    public void addWeight(double weight) {
        this.weight += weight;
    }

    public boolean isRecentSpill() {
        return recentSpill;
    }

    public void setRecentSpill(boolean recentSpill) {
        this.recentSpill = recentSpill;
    }

    public Operand getAlias() {
        if(alias == null)
            return this;
        return alias;
    }

    public void setAlias(Operand alias) {
        this.alias = alias;
    }

    public void addAdj(Operand adj) {
        adjOpdSet.add(adj);
    }

    public boolean isImm(){
        return this instanceof Imm;
    }

    public boolean needColor(String type) {
        if(type == "Integer") {
            return !isFloat && !(this instanceof Imm) && !(this instanceof Global);
        } else {
            assert type == "Float";
            return isFloat && !(this instanceof Imm) && !(this instanceof Global);
        }
    }

    public boolean isPreColored(String type) {
        if(type == "Integer") {
            return !isFloat && this instanceof PhyReg;
        } else {
            assert type == "Float";
            return isFloat && this instanceof FPhyReg;
        }
    }
    public boolean isFloat(){
        return isFloat;
    }

    public static class VirtualReg extends Operand{
        private static int vrCount = 0;
        private int value;
        public VirtualReg(boolean isFloat, McFunction mcFunction){
            this.value = vrCount++;
            this.isFloat = isFloat;
            if(isFloat)
                mcFunction.svrList.add(this);
            else
                mcFunction.vrList.add(this);
        }

        @Override
        public String toString() {
            return "v" + value;
        }
    }

    public static class Global extends Operand {
        private GlobalValue globalValue;
        private String name;
        public Global(GlobalValue globalValue){
            this.globalValue = globalValue;
            this.name = "global_" + globalValue.name.substring(1);
        }

        public GlobalValue getGlobalValue() {
            return globalValue;
        }

        @Override
        public String toString() {
            return name;
        }
    }


    public static class Imm extends Operand{
        private int intNumber;
        private float floatNumber;

        public Imm( int intNumber){
            this.isFloat = false;
            this.intNumber = intNumber;
            this.pre = "#";
        }

        public Imm(float floatNumber){
            this.isFloat = true;
            this.floatNumber = floatNumber;
            this.pre = "#";
        }

        public int getIntNumber() {
            return intNumber;
        }

        public float getFloatNumber() {
            return floatNumber;
        }

        @Override
        public String toString() {
            if(isFloat) {
                return "#" + Float.floatToRawIntBits(floatNumber);
            }
            return "#" + intNumber;

        }
    }

    public static class PhyReg extends Operand implements Comparable<PhyReg>{
        int idx;
        String name;
        private static HashMap<String, PhyReg> name2reg = new HashMap<>();
        private static HashMap<Integer, PhyReg> idx2reg = new HashMap<>();

        public static class PhyRegs{
            public static final PhyReg R0 = new PhyReg(0, "r0");
            public static final PhyReg R1 = new PhyReg(1, "r1");
            public static final PhyReg R2 = new PhyReg(2, "r2");
            public static final PhyReg R3 = new PhyReg(3, "r3");
            public static final PhyReg R4 = new PhyReg(4, "r4");
            public static final PhyReg R5 = new PhyReg(5, "r5");
            public static final PhyReg R6 = new PhyReg(6, "r6");
            public static final PhyReg R7 = new PhyReg(7, "r7");
            public static final PhyReg R8 = new PhyReg(8, "r8");
            public static final PhyReg R9 = new PhyReg(9, "r9");
            public static final PhyReg R10 = new PhyReg(10, "r10");
            public static final PhyReg R11 = new PhyReg(11, "r11");
            public static final PhyReg R12 = new PhyReg(12, "r12");
            public static final PhyReg SP = new PhyReg(13, "sp");
            public static final PhyReg LR = new PhyReg(14, "lr");
            public static final PhyReg PC = new PhyReg(15, "pc");
            public static final PhyReg CSPR = new PhyReg(16, "cspr");
        }

        static {
            for (Field field : Operand.PhyReg.PhyRegs.class.getDeclaredFields()) {
                try {
                    PhyReg phyReg= (PhyReg) field.get(Operand.PhyReg.PhyRegs.class);
                    name2reg.put(phyReg.name, phyReg);
                    idx2reg.put(phyReg.idx, phyReg);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        private PhyReg(int idx, String name){
            this.idx = idx;
            this.name = name;
            this.isFloat = false;
            this.phyReg = this;
        }

        public int getIdx() {
            return idx;
        }

        public static PhyReg getPhyReg(String name){
            return name2reg.get(name);
        }

        public static PhyReg getPhyReg(int idx) {
            return  idx2reg.get(idx);
        }

        public static TreeSet<Operand> getOkColorList() {
            TreeSet<Operand> okColorList = new TreeSet<>();
            for(int i = 0; i < 17; i++){
                okColorList.add(idx2reg.get(i));
            }
            okColorList.remove(name2reg.get("sp"));
            okColorList.remove(name2reg.get("pc"));
            okColorList.remove(name2reg.get("cspr"));
            return okColorList;
        }

        @Override
        public String toString() {
            return name;
        }

        public int compareTo(PhyReg o) {
            return  this.getIdx() - o.getIdx();
        }
    }

    public static class FPhyReg extends Operand implements Comparable<FPhyReg>{
        int idx;
        String name;
        public int degree = 0;
        private static HashMap<String, FPhyReg> name2reg = new HashMap<>();
        private static HashMap<Integer, FPhyReg> idx2reg = new HashMap<>();
        private static ArrayList<Operand> colorList = new ArrayList<>();

        private FPhyReg(int idx, String name){
            this.idx = idx;
            this.name = name;
            this.isFloat = true;
            this.phyReg = this;
        }

        public int getIdx() {
            return idx;
        }

        public static FPhyReg getFPhyReg(String name){
            return name2reg.get(name);
        }

        public static FPhyReg getFPhyReg(int idx) {
            return idx2reg.get(idx);
        }

        public static TreeSet<Operand> getOkColorList() {
            TreeSet<Operand> okColorList = new TreeSet<>(colorList);
            return okColorList;
        }

        @Override
        public String toString() {
            return name;
        }

        public static class FPhyRegs{
            public static final FPhyReg S0 = new FPhyReg(0, "s0");
            public static final FPhyReg S1 = new FPhyReg(1, "s1");
            public static final FPhyReg S2 = new FPhyReg(2, "s2");
            public static final FPhyReg S3 = new FPhyReg(3, "s3");
            public static final FPhyReg S4 = new FPhyReg(4, "s4");
            public static final FPhyReg S5 = new FPhyReg(5, "s5");
            public static final FPhyReg S6 = new FPhyReg(6, "s6");
            public static final FPhyReg S7 = new FPhyReg(7, "s7");
            public static final FPhyReg S8 = new FPhyReg(8, "s8");
            public static final FPhyReg S9 = new FPhyReg(9, "s9");
            public static final FPhyReg S10 = new FPhyReg(10, "s10");
            public static final FPhyReg S11 = new FPhyReg(11, "s11");
            public static final FPhyReg S12 = new FPhyReg(12, "s12");
            public static final FPhyReg S13 = new FPhyReg( 13, "s13");
            public static final FPhyReg S14 = new FPhyReg( 14, "s14");
            public static final FPhyReg S15 = new FPhyReg( 15, "s15");
            public static final FPhyReg S16 = new FPhyReg( 16, "s16");
            public static final FPhyReg S17 = new FPhyReg( 17, "s17");
            public static final FPhyReg S18 = new FPhyReg( 18, "s18");
            public static final FPhyReg S19 = new FPhyReg( 19, "s19");
            public static final FPhyReg S20 = new FPhyReg( 20, "s20");
            public static final FPhyReg S21 = new FPhyReg( 21, "s21");
            public static final FPhyReg S22 = new FPhyReg( 22, "s22");
            public static final FPhyReg S23 = new FPhyReg( 23, "s23");
            public static final FPhyReg S24 = new FPhyReg( 24, "s24");
            public static final FPhyReg S25 = new FPhyReg( 25, "s25");
            public static final FPhyReg S26 = new FPhyReg( 26, "s26");
            public static final FPhyReg S27 = new FPhyReg( 27, "s27");
            public static final FPhyReg S28 = new FPhyReg( 28, "s28");
            public static final FPhyReg S29 = new FPhyReg( 29, "s29");
            public static final FPhyReg S30 = new FPhyReg( 30, "s30");
            public static final FPhyReg S31 = new FPhyReg( 31, "s31");
        }

        static {
            for (Field field : Operand.FPhyReg.FPhyRegs.class.getDeclaredFields()) {
                try {
                    FPhyReg fPhyReg = (FPhyReg) field.get(Operand.FPhyReg.FPhyRegs.class);
                    name2reg.put(fPhyReg.name, fPhyReg);
                    idx2reg.put(fPhyReg.idx, fPhyReg);
                    colorList.add(fPhyReg);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        public int compareTo(FPhyReg o) {
            return  this.getIdx() - o.getIdx();
        }
    }
}
