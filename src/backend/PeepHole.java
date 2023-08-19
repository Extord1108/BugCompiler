package backend;

import lir.McBlock;
import lir.McFunction;
import lir.Operand;
import lir.mcInstr.*;

import java.util.ArrayList;

public class PeepHole {
    private ArrayList<McFunction> functions;
    public PeepHole(ArrayList<McFunction> functions) {
        this.functions = functions;
    }

    public void run() {
        for (McFunction function: functions) {
            peepHole(function);
        }
    }

    private void peepHole(McFunction function){
        for(McBlock mb:function.getMcBlocks()){
            for(McInstr instr : mb.getMcInstrs()){
                McInstr nextInstr = instr.getNext() == mb.getMcInstrs().tail?McInstr.nop:(McInstr)instr.getNext();
                McInstr prevInstr = instr.getPrev() == mb.getMcInstrs().head?McInstr.nop:(McInstr)instr.getPrev();
                if(instr instanceof McMove){
                    optMove(instr, nextInstr);
                }else if(instr instanceof McBinary){
                    optBinary(instr);
                }else if(instr instanceof McJump){
                    optJump(instr);
                }else if(instr instanceof McLdr){
                    optLdr(instr, prevInstr);
                } else if(instr instanceof MCShift) {
                    optShift(instr, nextInstr);
                }
            }
        }
    }
    private void  optShift(McInstr instr, McInstr nextInstr){
        assert instr instanceof MCShift;
        MCShift mcShift = (MCShift) instr;
        if(nextInstr instanceof McBinary) {
            McBinary mcBinary = (McBinary) nextInstr;
            if(mcBinary.type.equals(McBinary.BinaryType.Add) || mcBinary.type.equals(McBinary.BinaryType.Sub)) {
                if(mcBinary.getSrc2().equals(mcShift.defOperands.get(0))) {
                    McInstr mcInstr = new McBinary(mcBinary.type, mcBinary.getDst(), mcBinary.getSrc1(), mcShift.useOperands.get(0),
                        new MCShift(mcShift.getShiftType(), mcShift.useOperands.get(1)), mcShift.mcBlock, false);
                    mcShift.mcBlock.getMcInstrs().insertBefore(mcShift, mcInstr);
                    mcShift.remove();
                    mcBinary.remove();
                }
            }
        }
    }

    private void  optMove(McInstr instr, McInstr nextInstr){
        assert instr instanceof McMove;
        McMove move1 = (McMove)instr;
        if(move1.getDstOp().equals(move1.getSrcOp())){
            move1.remove();
        }else if(!move1.isCond() && nextInstr instanceof McMove){
            McMove move2 = (McMove)nextInstr;
            if(move2.getDstOp().equals(move2.getSrcOp())){
                move2.remove();
            }else if(move2.getDstOp().equals(move1.getDstOp())){
                move1.remove();
            }else if(move2.getSrcOp().equals(move1.getDstOp()) && move2.getDstOp().equals(move1.getSrcOp())){
                move2.remove();
            }
        }
    }

    private void optBinary(McInstr instr){
        assert instr instanceof McBinary;
        McBinary binary = (McBinary)instr;
        if(binary.type.equals(McBinary.BinaryType.Add) || binary.type.equals(McBinary.BinaryType.Sub)){
            if(binary.getSrc2() instanceof Operand.Imm){
                int imm = ((Operand.Imm)binary.getSrc2()).getIntNumber();
                if(imm == 0){
                    if(binary.getDst().equals(binary.getSrc1())) {
                        binary.remove();
                    } else{
                        McBlock mb = binary.mcBlock;
                        mb.getMcInstrs().insertBefore(binary, new McMove(binary.getDst(), binary.getSrc1(), binary.mcBlock,false));
                        binary.remove();
                    }
                }
            }
        }
    }

    private void optJump(McInstr instr){
        assert instr instanceof McJump;
        McJump jump = (McJump)instr;
        if(jump.getTarget().equals(jump.mcBlock.getNext())){
            jump.remove();
        }
    }

    private void optLdr(McInstr instr, McInstr prevInstr){
        assert instr instanceof McLdr;
        McLdr ldr = (McLdr)instr;
        if(prevInstr instanceof McStore){
            McStore store = (McStore)prevInstr;
            if(ldr.getAddr().equals(store.getAddr())){
                if(ldr.getOffset()==null){
                    if(store.getOffset()==null)
                    {
                        ldr.mcBlock.getMcInstrs().insertBefore(ldr, new McMove(ldr.getData(), store.getData(), store.mcBlock,false));
                        ldr.remove();
                    }
                }else{
                    if(store.getOffset()!=null && ldr.getOffset().equals(store.getOffset()))
                    {
                        ldr.mcBlock.getMcInstrs().insertBefore(ldr, new McMove(ldr.getData(), store.getData(), store.mcBlock,false));
                        ldr.remove();
                    }
                }
            }
        }

    }
}
