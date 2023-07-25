package frontend.semantic;

import ir.Value;
import ir.type.Type;

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

    @Override
    public String toString() {
        return type + " " + value;
    }
}
