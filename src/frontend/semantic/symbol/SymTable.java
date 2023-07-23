package frontend.semantic.symbol;

import java.util.HashMap;

public class SymTable {
    private final HashMap< String, Symbol> symbols = new HashMap<>();
    private final SymTable parent;

    public SymTable(SymTable symTable){
        this.parent = symTable;
    }

    public SymTable getParent(){
        return parent;
    }

    public void add(Symbol symbol){
        if(symbols.containsKey(symbol.getName())){
            System.err.println("Current symbol table contains " + symbol.getName());
            return;
        }
        symbols.put(symbol.getName(), symbol);
    }

    public Symbol get(String name, boolean recursion){
        Symbol ret = symbols.get(name);
        if(ret == null && parent != null && recursion){
            ret = parent.get(name, recursion);
        }
        return ret;
    }

}
