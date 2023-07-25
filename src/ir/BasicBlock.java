package ir;

import ir.instruction.Instr;
import util.MyList;

import java.util.ArrayList;

public class BasicBlock extends Value{
    ArrayList<Instr> instrs = new ArrayList<>();

    public void addInstr(Instr instr){
        instrs.add(instr);
    }
}
