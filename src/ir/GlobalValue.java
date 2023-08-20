package ir;

import frontend.semantic.InitVal;
import ir.instruction.Instr;
import ir.type.PointerType;
import ir.type.Type;

import java.util.HashSet;

public class GlobalValue extends Value {
    public InitVal initVal;
    private HashSet<Instr> loads = new HashSet<>();

    public GlobalValue(String name, Type type, InitVal initVal) {
        this.type = new PointerType(type);
        this.name = "@" + name;
        this.initVal = initVal;
    }

    public InitVal getInitVal() {
        return initVal;
    }

    public HashSet<Instr> getLoads(){
        return loads;
    }

    public void cleanLoad(){
        this.loads.clear();
    }

    public void addLoad(Instr load){
        this.loads.add(load);
    }

    @Override
    public String toString() {
        return name + " = dso_local global " + initVal.toString() + "\n";
    }

}
