package ir.type;

public class PointerType extends Type{

    public PointerType(Type basicType){
        this.basicType = basicType;
    }

    @Override
    public String toString() {
        return basicType + "*";
    }

    public Type getBasicType() {
        return basicType;
    }

    public Type getContentType(){
        if(basicType instanceof ArrayType){
            return ((ArrayType)basicType).getContextType();
        }else{
            return basicType;
        }
    }
}
