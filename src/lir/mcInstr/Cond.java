package lir.mcInstr;

// https://developer.arm.com/documentation/dui0489/i/arm-and-thumb-instructions/condition-codes?lang=en
public enum Cond {
    Eq("eq"),
    Ne("ne"),
    Gt("gt"),
    Ge("ge"),
    Lt("lt"),
    Le("le"),
    Hi("hi"), // >
    Pl("pl"),
    Any(""),
    ;

    String name;
    Cond(String cond) {
        this.name = cond;
    }

    @Override
    public String toString() {
        return name;
    }
}
