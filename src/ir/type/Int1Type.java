package ir.type;

public class Int1Type extends Type {
    private static Int1Type instance = null;

    private Int1Type() {
    }

    public static Int1Type getInstance() {
        if (instance == null)
            instance = new Int1Type();
        return instance;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Int1Type;
    }

    @Override
    public String toString() {
        return "i1";
    }

}
