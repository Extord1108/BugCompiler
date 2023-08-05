package ir;

import frontend.semantic.InitVal;
import ir.type.PointerType;
import ir.type.Type;

public class GlobalValue extends Value {
    public InitVal initVal;

    public GlobalValue(String name, Type type, InitVal initVal) {
        this.type = new PointerType(type);
        this.name = "@" + name;
        this.initVal = initVal;
    }

    public InitVal getInitVal() {
        return initVal;
    }

    @Override
    public String toString() {
        return name + " = dso_local global " + initVal.toString() + "\n";
    }

}
