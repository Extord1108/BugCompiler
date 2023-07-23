package ir.type;

public class ArrayType extends Type{
    private final Type basicType;
    private final int size;

    public ArrayType(Type basicType, int size){
        this.basicType = basicType;
        this.size = size;
    }

    @Override
    public String toString() {
        return "[" + size + " x " + basicType + "]";
    }

    public int getSize() {
        return size;
    }

    public Type getBasicType() {
        return basicType;
    }

    public int getFattenSize(){
        if(basicType instanceof ArrayType){
            return size * ((ArrayType)basicType).getFattenSize();
        }else{
            return size;
        }
    }
}
