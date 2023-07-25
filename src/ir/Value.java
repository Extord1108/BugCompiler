package ir;

import ir.type.Type;
import util.MyNode;

public class Value extends MyNode {
    public Type type;
    public String name;
    public String pre;

    public static final String GLOBAL_PRE = "@";
    public static final String LOCAL_PRE = "%";

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}
