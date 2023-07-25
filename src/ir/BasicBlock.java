package ir;

import ir.instruction.Instr;
import ir.instruction.Jump;
import ir.instruction.Return;
import util.MyList;

import java.util.ArrayList;

public class BasicBlock extends Value {
    MyList<Instr> instrs = new MyList<>();

    public void addInstr(Instr instr) {
        instrs.insertTail(instr);
    }

    public boolean isTerminated() {
        return getLast() instanceof Return;
    }

    public Instr getLast() {
        return instrs.getLast();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(":");
        for (Instr instr : instrs) {
            sb.append(instr.toString());
        }
        return sb.toString();
    }
}
