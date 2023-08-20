package ir.instruction;

import ir.BasicBlock;
import ir.type.ArrayType;
import ir.type.PointerType;
import ir.type.Type;

import java.util.HashSet;

// 内存分配
public class Alloc extends Instr {

    private HashSet<Instr> loads = new HashSet<>();

    public Alloc(Type type, BasicBlock basicBlock) {
        super(new PointerType(type), basicBlock, true);
    }

    public boolean isArrayAlloc() {
        return this.type.getBasicType() instanceof ArrayType;
    }

    @Override
    public Instr clone(BasicBlock bb){
        this.cloneInstr = new Alloc(this.type.getBasicType(), bb);
        return this.cloneInstr;
    }

    public void setLoads( HashSet<Instr> loads){
        this.loads = loads;
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
        return this.getName() + " = alloca " + this.type.getBasicType().toString();
    }
}
