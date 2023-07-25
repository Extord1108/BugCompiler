package ir.type;

public class VoidType extends Type{
    private static VoidType instance = null;
    private VoidType(){
    }

    public static VoidType getInstance(){
        if(instance == null)
            instance = new VoidType();
        return instance;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof VoidType;
    }
    @Override
    public String toString() {
        return "void";
    }
}
