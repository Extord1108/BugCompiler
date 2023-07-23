package frontend.semantic;

public class ConstNumber {
    private Integer intValue;
    private Float floatValue;
    private NumberType type;

    public ConstNumber(Object number){
        setNumber(number);
    }

    public Object getNumber() {
        Object ret = null;
        if(type == NumberType.Float){
            ret = floatValue;
        }else{
            ret = intValue;
        }
        return ret;
    }

    public void setNumber(Object number){
        if(number instanceof Float){
            this.type = NumberType.Float;
            this.floatValue = (Float) number;
        }else if(number instanceof Integer){
            this.type = NumberType.INT;
            this.intValue = (Integer) number;
        }
    }

    public NumberType getType() {
        return type;
    }

    public enum NumberType{
        Float,
        INT,
    }
}
