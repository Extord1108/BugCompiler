package ir.type;

public class PointerType extends Type{
    private final Type basicType;

    public PointerType(Type basicType){
        this.basicType = basicType;
    }

    @Override
    public String toString() {
        return basicType + "*";
    }
}
