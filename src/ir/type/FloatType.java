package ir.type;

public class FloatType extends Type{

    private static FloatType instance = null;
    private FloatType(){
    }

    public static FloatType getInstance(){
        if(instance == null)
            instance = new FloatType();
        return instance;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FloatType;
    }
    @Override
    public String toString() {
        return "float";
    }
}
