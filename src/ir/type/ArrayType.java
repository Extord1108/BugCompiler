package ir.type;

public class ArrayType extends Type{
    private final Type basicType;
    private final int size;

    public ArrayType(Type basicType, int size){
        this.basicType = basicType;
        this.size = size;
    }
}
