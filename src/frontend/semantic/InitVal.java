package frontend.semantic;

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

    @Override
    public String toString() {
        return type + " " + value;
    }
}
