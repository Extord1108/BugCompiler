package frontend.semantic;

import ir.Constant;
import ir.Value;
import ir.type.Type;

public class InitVal {
    public final Type type;
    public final Value value;

    public InitVal(Type type,Value value){
        this.type = type;
        this.value = value;
    }

    @Override
    public String toString() {
        return type + " " + value;
    }
}
