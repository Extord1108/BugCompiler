package ir.type;

public class ArrayType extends Type{
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

    public int getDims(){
        if(basicType instanceof ArrayType){
            return 1 + ((ArrayType)basicType).getDims();
        }
        return 1;
    }

    public int getFattenSize(){
        if(basicType instanceof ArrayType){
            return size * ((ArrayType)basicType).getFattenSize();
        }else{
            return size;
        }
    }

    public Type getContextType(){
        if(basicType instanceof ArrayType){
            return ((ArrayType)basicType).getContextType();
        }else{
            return basicType;
        }
    }
}
