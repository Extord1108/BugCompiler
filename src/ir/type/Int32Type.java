package ir.type;

public class Int32Type extends Type{

    private static Int32Type instance = null;
    private Int32Type(){
    }

    public static Int32Type getInstance(){
        if(instance == null)
            instance = new Int32Type();
        return instance;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Int32Type;
    }
    @Override
    public String toString() {
        return "i32";
    }
}
