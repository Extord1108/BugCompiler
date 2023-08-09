package frontend.semantic;

import com.sun.jdi.FloatType;
import ir.Value;
import ir.Variable;
import ir.type.ArrayType;
import ir.type.Type;
import util.MyList;
import util.MyNode;

import java.util.ArrayList;

public class InitVal {
    public final Type type;
    public final Value value;

    public InitVal(Type type,Value value){
        this.type = type;
        this.value = value;
    }

    public Type getType() {
        return type;
    }

    public Value getValue() {
        return value;
    }

    public ArrayList<Value> flatten(){
        assert type instanceof ArrayType;
        return  ((Variable.VarArray) value).flatten();
    }

    public String armOut() {
        if(value instanceof Variable.ConstInt)
            return "\t.word\t" + ((Variable.ConstInt) value).getIntVal() + "\n";
        else if(value instanceof Variable.ConstFloat)
            return "\t.word\t" + Float.floatToIntBits(((Variable.ConstFloat) value).getFloatVal()) + "\n";
        else if(value instanceof Variable.ZeroInit)
            return "\t.zero\t" + (((ArrayType)value.getType()).getFattenSize() * 4) + "\n";
        else{
            assert value instanceof Variable.VarArray;
            StringBuilder stb = new StringBuilder();
            int zeroSize = 0;
            ArrayList<Value> flatten = ((Variable.VarArray) value).flatten();
            for(int i = 0; i < flatten.size(); i++) {
                Value  st = flatten.get(i);
                if(st instanceof Variable.ConstInt) {
                    if(((Variable.ConstInt) st).getIntVal() == 0) {
                        zeroSize += 4;
                    } else {
                        if(zeroSize != 0) {
                            stb.append("\t.zero\t" + zeroSize).append("\n");
                            zeroSize = 0;
                        }
                        stb.append("\t.word\t").append(((Variable.ConstInt) st).getIntVal()).append("\n");
                    }
                } else if(st instanceof Variable.ConstFloat) {
                    if(((Variable.ConstFloat) st).getFloatVal() == 0.0) {
                        zeroSize += 4;
                    } else {
                        if(zeroSize != 0) {
                            stb.append("\t.zero\t" + zeroSize).append("\n");
                            zeroSize = 0;
                        }
                        stb.append("\t.word\t").append(Float.floatToIntBits(((Variable.ConstFloat) st).getFloatVal())).append("\n");
                    }
                } else {
                    assert st instanceof Variable.Undef;
                    zeroSize += 4;
                }
            }
            if(zeroSize != 0) {
                stb.append("\t.zero\t" + zeroSize).append("\n");
                zeroSize = 0;
            }
            return stb.toString();
        }
    }

    @Override
    public String toString() {
        return type + " " + value;
    }
}
