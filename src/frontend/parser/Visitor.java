package frontend.parser;

import frontend.semantic.Evaluate;
import frontend.semantic.InitVal;
import frontend.semantic.OpTree;
import frontend.semantic.OpTreeHandler;
import frontend.semantic.symbol.SymTable;
import frontend.semantic.symbol.Symbol;
import ir.*;
import ir.instruction.*;
import ir.type.*;
import manager.Manager;
import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;

import java.util.ArrayList;
import java.util.Stack;

public class Visitor extends AbstractParseTreeVisitor<Value> implements SysYVisitor<Value> {
    public static final Visitor Instance = new Visitor();

    private Visitor() {
    }

    private final Manager manager = Manager.getManager();

    private Type defContextType = null;
    private OpTree current = new OpTree(new ArrayList<>(), new ArrayList<>(), null, null);
    private ArrayList<Function.Param> curFuncParams = null;

    private SymTable curSymTable = new SymTable(null);
    private BasicBlock curBasicBlock = null;
    private Function curFunction = null;

    private Variable.ConstFloat CONST_0f = new Variable.ConstFloat(0.0f);
    private Variable.ConstInt CONST_0 = new Variable.ConstInt(0);

    private final Stack<BasicBlock> blockFollows = new Stack<>();
    private final Stack<BasicBlock> blockHeads = new Stack<>();
    private boolean needPointer = false;

    private boolean isGlobal() {
        return curBasicBlock == null;
    }

    public BasicBlock getCurBasicBlock() {
        return curBasicBlock;
    }

    public void setCurBasicBlock(BasicBlock curBasicBlock) {
        this.curBasicBlock = curBasicBlock;
    }

    public Variable.ConstInt getCONST_0() {
        return CONST_0;
    }

    public Value turnTo(Value value, Type targetType) {

        if (value.getType().equals(targetType)) {
            return value;
        } else {
            if (targetType instanceof Int32Type) {
                assert value.getType() instanceof FloatType || value.getType() instanceof Int1Type;
                if(value.getType() instanceof FloatType)
                    return new Fptosi(value, curBasicBlock);
                else
                    return new Zext(value, curBasicBlock);
            } else if (targetType instanceof FloatType) {
                assert value.getType() instanceof Int32Type || value.getType() instanceof Int1Type;
                if(value.getType() instanceof Int32Type)
                    return new Sitofp(value, curBasicBlock);
                else{
                    value = new Zext(value, curBasicBlock);
                    return new Sitofp(value, curBasicBlock);
                }
            } else {
                if (value.getType().equals(Int32Type.getInstance())) {
                    return new Icmp(value, new Variable.ConstInt(0), OpTree.Operator.Ne, curBasicBlock);
                } else {
                    assert value.getType().equals(FloatType.getInstance());
                    return new Fcmp(value, new Variable.ConstFloat(0), OpTree.Operator.Ne, curBasicBlock);
                }
            }

        }

    }

    @Override
    public Value visitCompUnit(SysYParser.CompUnitContext ctx) {
        //System.out.println("visitCompUnit");
        visitChildren(ctx);
        return null;
    }

    @Override
    public Value visitDecl(SysYParser.DeclContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override
    public Value visitConstDecl(SysYParser.ConstDeclContext ctx) {
        if (ctx.bType().INT() != null) {
            defContextType = Int32Type.getInstance();
        } else if (ctx.bType().FLOAT() != null) {
            defContextType = FloatType.getInstance();
        } else {
            throw new Error("syntax error: var decl using undefinedType");
        }
        visitChildren(ctx);
        return null;
    }

    @Override
    public Value visitBType(SysYParser.BTypeContext ctx) {
        return null;
    }

    @Override
    public Value visitConstDef(SysYParser.ConstDefContext ctx) {
        String ident = ctx.IDENT().getText();
        Type currentType = defContextType;
        InitVal initVal = null;
        // 每一维数组的长度
        ArrayList<Integer> lengths = new ArrayList<>();
        for (var exp : ctx.constExp()) {
            visit(exp);
            lengths.add((Integer) Evaluate.evalConstExp(current.getLast()));
        }
        for (int i = lengths.size() - 1; i >= 0; i--) {
            currentType = new ArrayType(currentType, lengths.get(i));
        }
        // 获得初始化值的相关数据
        if (currentType instanceof ArrayType) {
            Variable.VarArray constArray = (Variable.VarArray) visit(ctx.constInitVal());
            if(constArray.getSize() == 0){
                initVal = new InitVal(currentType, new Variable.ZeroInit(currentType));
            }else {
                constArray = constArray.changeType((ArrayType) currentType);
                initVal = new InitVal(currentType, constArray);
            }
        } else {
            initVal = new InitVal(currentType, visit(ctx.constInitVal()));
        }
        // 进行初始化赋值相关工作
        Value pointer = null;
        if (!isGlobal()) {
            pointer = new Alloc(currentType, curBasicBlock);
            Type initType = initVal.getType();
            if (initType instanceof ArrayType) {
                ArrayList<Value> idxList = new ArrayList<>();
                for (int i = 0; i <= ((ArrayType) initType).getDims(); i++) {
                    idxList.add(CONST_0);
                }
                Type contextType = ((ArrayType) initType).getContextType();
                Value pl = new GetElementPtr(contextType, pointer, idxList, curBasicBlock);
                if(initVal.getValue() instanceof Variable.VarArray){
                    ArrayList<Value> flatten = initVal.flatten();
                    new Store(turnTo(flatten.get(0), contextType), pl, curBasicBlock);
                    for (int i = 1; i < flatten.size(); i++) {
                        idxList = new ArrayList<>();
                        idxList.add(new Variable.ConstInt(i));
                        Value p = new GetElementPtr(contextType, pl, idxList, curBasicBlock);
                        new Store(turnTo(flatten.get(i), contextType), p, curBasicBlock);
                    }
                }else {
                    assert initVal.getValue() instanceof Variable.ZeroInit;
                    ArrayList<Value> params = new ArrayList<>();
                    Value newPl = pl;
                    if(contextType instanceof FloatType){
                        newPl = new BitCast( pl, new PointerType(Int32Type.getInstance()), curBasicBlock);
                    }
                    params.add(newPl);
                    params.add(CONST_0);
                    params.add(new Variable.ConstInt(((ArrayType) initType).getFattenSize() * 4));
                    new Call(Manager.ExternFunction.MEM_SET, params, curBasicBlock);
                }
            } else if ((initType instanceof FloatType) || (initType instanceof Int32Type)) {
                Value value = turnTo(initVal.getValue(), initType);
                new Store(value, pointer, curBasicBlock);
            }
        } else {
            pointer = new GlobalValue(ident, currentType, initVal);
        }
        // 将对应的符号加入到符号表中
        Symbol symbol = new Symbol(ident, defContextType, true, initVal, pointer);
        curSymTable.add(symbol);
        if (isGlobal()) {
            manager.addGlobal((GlobalValue) pointer);
        }
        return null;
    }

    @Override
    public Value visitConstInitVal(SysYParser.ConstInitValContext ctx) {
        Value ret;
        if (ctx.constExp() != null) {
            visit(ctx.constExp());
            var number = Evaluate.evalConstExp(current.getLast());
            if (number instanceof Integer) {
                if (defContextType instanceof Int32Type) {
                    return new Variable.ConstInt((int) number);
                } else {
                    assert defContextType instanceof FloatType;
                    return new Variable.ConstFloat((float) ((int) number));
                }
            } else {
                assert number instanceof Float;
                if (defContextType instanceof Int32Type) {
                    return new Variable.ConstInt((int) ((float) number));
                } else {
                    assert defContextType instanceof FloatType;
                    return new Variable.ConstFloat((float) number);
                }
            }
        } else {
            ret = new Variable.VarArray(null);
            for (int i = 0; i < ctx.constInitVal().size(); i++) {
                ((Variable.VarArray) ret).add((Variable) visit(ctx.constInitVal(i)));
            }
        }
        return ret;
    }

    @Override
    public Value visitVarDecl(SysYParser.VarDeclContext ctx) {
        if (ctx.bType().INT() != null) {
            defContextType = Int32Type.getInstance();
        } else if (ctx.bType().FLOAT() != null) {
            defContextType = FloatType.getInstance();
        } else {
            throw new Error("syntax error: var decl using undefinedType");
        }
        visitChildren(ctx);
        return null;
    }

    @Override
    public Value visitVarDef(SysYParser.VarDefContext ctx) {
        String ident = ctx.IDENT().getText();
        Type currentType = defContextType;
        InitVal initVal = null;
        // 每一维数组的长度
        ArrayList<Integer> lengths = new ArrayList<>();
        for (var exp : ctx.constExp()) {
            visit(exp);
            lengths.add((Integer) Evaluate.evalConstExp(current.getLast()));
        }
        for (int i = lengths.size() - 1; i >= 0; i--) {
            currentType = new ArrayType(currentType, lengths.get(i));
        }
        // 判断是否初始化
        if (ctx.initVal() != null) {
            if (currentType instanceof ArrayType) {
                Variable.VarArray varArray = (Variable.VarArray) visit(ctx.initVal());
                if(varArray.getSize() == 0){
                    initVal = new InitVal(currentType, new Variable.ZeroInit(currentType));
                } else{
                    if(!isGlobal())
                        varArray.setAddZero(false);
                    varArray = varArray.changeType((ArrayType) currentType);
                    initVal = new InitVal(currentType, varArray);
                }
            } else {
                initVal = new InitVal(currentType, visit(ctx.initVal()));
            }
        } else {
            // Global情况下补0,非global的情况下值为不确定的值,init为null
            if (isGlobal()) {
                if (currentType instanceof ArrayType) {
                    initVal = new InitVal(currentType, new Variable.ZeroInit(currentType));
                } else if (currentType instanceof Int32Type) {
                    initVal = new InitVal(currentType, CONST_0);
                } else {
                    assert currentType instanceof FloatType;
                    initVal = new InitVal(currentType, CONST_0f);
                }
            }
        }

        Value pointer = null;
        if (!isGlobal()) {
            pointer = new Alloc(currentType, curBasicBlock);
            if (initVal != null) {
                Type initType = initVal.getType();
                if (initType instanceof ArrayType) {
                    ArrayList<Value> idxList = new ArrayList<>();
                    for (int i = 0; i <= ((ArrayType) initType).getDims(); i++) {
                        idxList.add(CONST_0);
                    }
                    Type contextType = ((ArrayType) initType).getContextType();
                    Value pl = new GetElementPtr(contextType, pointer, idxList, curBasicBlock);
                    if(initVal.getValue() instanceof Variable.VarArray){
                        ArrayList<Value> flatten = initVal.flatten();
                        // 存在undef需要memset一次
                        for(int i = 0; i < flatten.size(); i++){
                            if(flatten.get(i) instanceof Variable.Undef){
                                ArrayList<Value> params = new ArrayList<>();
                                Value newPl = pl;
                                if(contextType instanceof FloatType){
                                    newPl = new BitCast( pl, new PointerType(Int32Type.getInstance()), curBasicBlock);
                                }
                                params.add(newPl);
                                params.add(CONST_0);
                                params.add(new Variable.ConstInt(((ArrayType) initType).getFattenSize() * 4));
                                new Call(Manager.ExternFunction.MEM_SET, params, curBasicBlock);
                                break;
                            }
                        }
                        if(!(flatten.get(0) instanceof Variable.Undef))
                            new Store(turnTo(flatten.get(0), contextType), pl, curBasicBlock);
                        for (int i = 1; i < flatten.size(); i++) {
                            idxList = new ArrayList<>();
                            idxList.add(new Variable.ConstInt(i));
                            if(!(flatten.get(i) instanceof Variable.Undef)){
                                Value p = new GetElementPtr(contextType, pl, idxList, curBasicBlock);
                                new Store(turnTo(flatten.get(i), contextType), p, curBasicBlock);
                            }
                        }
                    }else {
                        assert initVal.getValue() instanceof Variable.ZeroInit;
                        ArrayList<Value> params = new ArrayList<>();
                        Value newPl = pl;
                        if(contextType instanceof FloatType){
                            newPl = new BitCast( pl, new PointerType(Int32Type.getInstance()), curBasicBlock);
                        }
                        params.add(newPl);
                        params.add(CONST_0);
                        params.add(new Variable.ConstInt(((ArrayType) initType).getFattenSize() * 4));
                        new Call(Manager.ExternFunction.MEM_SET, params, curBasicBlock);
                    }
                }
                else if ((initType instanceof FloatType) || (initType instanceof Int32Type)) {
                    Value value = turnTo(initVal.getValue(), initType);
                    new Store(value, pointer, curBasicBlock);
                }
            }
        } else {
            pointer = new GlobalValue(ident, currentType, initVal);
        }
        Symbol symbol = new Symbol(ident, defContextType, false, initVal, pointer);
        curSymTable.add(symbol);
        if (isGlobal()) {
            manager.addGlobal((GlobalValue) pointer);
        }
        return null;
    }

    @Override
    public Value visitInitVal(SysYParser.InitValContext ctx) {
        Value ret;
        if (ctx.exp() != null) {
            visit(ctx.exp());
            if (isGlobal()) {
                var number = Evaluate.evalConstExp(current.getLast());
                if (number instanceof Integer) {
                    if (defContextType instanceof Int32Type) {
                        ret = new Variable.ConstInt((int) number);
                    } else {
                        assert defContextType instanceof FloatType;
                        ret = new Variable.ConstFloat((float) ((int) number));
                    }
                } else {
                    assert number instanceof Float;
                    if (defContextType instanceof Int32Type) {
                        ret = new Variable.ConstInt((int) ((float) number));
                    } else {
                        assert defContextType instanceof FloatType;
                        ret = new Variable.ConstFloat((float) number);
                    }
                }
            } else {
                ret = OpTreeHandler.evalExp(current.getLast(), curBasicBlock);
            }
        } else {
            ret = new Variable.VarArray(null);
            for (int i = 0; i < ctx.initVal().size(); i++) {
                ((Variable.VarArray) ret).add(visit(ctx.initVal(i)));
            }
        }
        return ret;
    }

    @Override
    public Value visitFuncDef(SysYParser.FuncDefContext ctx) {
        visit(ctx.funcType());
        Type returnType = defContextType;
        var ident = ctx.IDENT().getText();
        assert manager.hasFunction(ident) == false;

        BasicBlock entry = new BasicBlock();
        curBasicBlock = entry;
        curSymTable = new SymTable(curSymTable);
        curFuncParams = new ArrayList<>();
        Function function = new Function(ident, curFuncParams, returnType);
        manager.addFunction(function);
        entry.addFunction(function);
        if (ctx.funcFParams() != null) {
            visit(ctx.funcFParams());
            for (Function.Param param : curFuncParams) {
                Value paramPointer = new Alloc(param.getType(), curBasicBlock);
                new Store(param, paramPointer, curBasicBlock);
                curSymTable.add(new Symbol(param.getParamName(), param.getType(), false, null, paramPointer));
            }
        }
        curFunction = function;
        visit(ctx.block());
//        if(curBasicBlock.getInstrs().size() == 0){
//
//        }
        if (!curBasicBlock.isTerminated()) {
            if (returnType instanceof VoidType) {
                new Return(curBasicBlock);
            } else if (returnType instanceof Int32Type) {
                new Return(CONST_0, curBasicBlock);
            } else {
                assert returnType instanceof FloatType;
                new Return(CONST_0f, curBasicBlock);
            }
        }

        curBasicBlock = null;
        curFunction = null;
        curSymTable = curSymTable.getParent();
        return null;
    }

    @Override
    public Value visitFuncType(SysYParser.FuncTypeContext ctx) {
        if (ctx.INT() != null) {
            defContextType = Int32Type.getInstance();
        } else if (ctx.FLOAT() != null) {
            defContextType = FloatType.getInstance();
        } else {
            assert ctx.VOID() != null;
            defContextType = VoidType.getInstance();
        }
        return null;
    }

    @Override
    public Value visitFuncFParams(SysYParser.FuncFParamsContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override
    public Value visitFuncFParam(SysYParser.FuncFParamContext ctx) {
        Type curType = null;
        String ident = ctx.IDENT().getText();
        if (ctx.bType().INT() != null) {
            curType = Int32Type.getInstance();
        } else {
            assert ctx.bType().FLOAT() != null;
            curType = FloatType.getInstance();
        }
        if (ctx.LBRACK().size() != 0) {
            ArrayList<Integer> lengths = new ArrayList<>();
            for (var exp : ctx.constExp()) {
                visit(exp);
                lengths.add((Integer) Evaluate.evalConstExp(current.getLast()));
            }
            for (int i = lengths.size() - 1; i >= 0; i--) {
                curType = new ArrayType(curType, lengths.get(i));
            }
            curType = new PointerType(curType);
        }
        curFuncParams.add(new Function.Param(ident, curType));
        return null;
    }

    @Override
    public Value visitBlock(SysYParser.BlockContext ctx) {
        curSymTable = new SymTable(curSymTable);
        visitChildren(ctx);
        curSymTable = curSymTable.getParent();
        return null;
    }

    @Override
    public Value visitBlockItem(SysYParser.BlockItemContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override
    public Value visitStmt(SysYParser.StmtContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override
    public Value visitExpStmt(SysYParser.ExpStmtContext ctx) {
        if (ctx.exp() != null) {
            visit(ctx.exp());
            OpTreeHandler.evalExp(current.getLast(), curBasicBlock);
        }
        return null;
    }

    @Override
    public Value visitAssignStmt(SysYParser.AssignStmtContext ctx) {
        needPointer = true;
        Value left = visit(ctx.lVal());
        visit(ctx.exp());
        Value right = OpTreeHandler.evalExp(current.getLast(), curBasicBlock);
        assert left.type instanceof PointerType;
        right = turnTo(right, ((PointerType) left.type).getContentType());
        new Store(right, left, curBasicBlock);
        return null;
    }

    @Override
    public Value visitIfStmt(SysYParser.IfStmtContext ctx) {
        BasicBlock thenBlock = new BasicBlock(curFunction);
        BasicBlock followBlock;
        if (ctx.stmt().size() == 1) {
            visit(ctx.cond());
            followBlock = new BasicBlock(curFunction);
            Value cond = OpTreeHandler.evalCond(current.getLast(), thenBlock, followBlock);
            cond = turnTo(cond, Int1Type.getInstance());
            new Branch(cond, thenBlock, followBlock, curBasicBlock);
            curBasicBlock = thenBlock;
            visit(ctx.stmt(0));
        } else {
            assert ctx.stmt().size() == 2;
            BasicBlock elseBlock = new BasicBlock(curFunction);
            followBlock = new BasicBlock(curFunction);
            visit(ctx.cond());
            Value cond = OpTreeHandler.evalCond(current.getLast(), thenBlock, elseBlock);
            cond = turnTo(cond, Int1Type.getInstance());
            new Branch(cond, thenBlock, elseBlock, curBasicBlock);
            curBasicBlock = thenBlock;
            visit(ctx.stmt(0));
            new Jump(followBlock, curBasicBlock);
            curBasicBlock = elseBlock;
            visit(ctx.stmt(1));
        }
        new Jump(followBlock, curBasicBlock);
        curBasicBlock = followBlock;
        return null;
    }

    @Override
    public Value visitWhileStmt(SysYParser.WhileStmtContext ctx) {
        BasicBlock condBlock = new BasicBlock(curFunction);
        BasicBlock bodyBlock = new BasicBlock(curFunction);
        BasicBlock followBlock = new BasicBlock(curFunction);
        new Jump(condBlock, curBasicBlock);
        curBasicBlock = condBlock;
        visit(ctx.cond());
        Value cond = OpTreeHandler.evalCond(current.getLast(), bodyBlock, followBlock);
        cond = turnTo(cond, Int1Type.getInstance());
        new Branch(cond, bodyBlock, followBlock, curBasicBlock);
        curBasicBlock = bodyBlock;
        blockHeads.push(condBlock);
        blockFollows.push(followBlock);
        visit(ctx.stmt());
        blockHeads.pop();
        blockFollows.pop();
        new Jump(condBlock, curBasicBlock);
        curBasicBlock = followBlock;
        return null;
    }

    @Override
    public Value visitBreakStmt(SysYParser.BreakStmtContext ctx) {
        assert !blockFollows.empty();
        new Jump(blockFollows.peek(), curBasicBlock);
        return null;
    }

    @Override
    public Value visitContinueStmt(SysYParser.ContinueStmtContext ctx) {
        assert !blockHeads.empty();
        new Jump(blockHeads.peek(), curBasicBlock);
        return null;
    }

    @Override
    public Value visitReturnStmt(SysYParser.ReturnStmtContext ctx) {
        if (ctx.exp() == null) {
            new Return(curBasicBlock);
        } else {
            visit(ctx.exp());
            Value value = OpTreeHandler.evalExp(current.getLast(), curBasicBlock);
            value = turnTo(value, curFunction.getType());
            new Return(value, curBasicBlock);
        }
        return null;
    }

    @Override
    public Value visitExp(SysYParser.ExpContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override
    public Value visitCond(SysYParser.CondContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override
    public Value visitLVal(SysYParser.LValContext ctx) {
        boolean needPointer = this.needPointer;
        this.needPointer = false;
        String ident = ctx.IDENT().getText();
        Symbol symbol = curSymTable.get(ident, true);
        // 针对只是一个数的情况取值
        if (ctx.exp().size() == 0 && (!(symbol.getValue().getType().getBasicType() instanceof ArrayType))) {
            if (needPointer) {
                return symbol.getValue();
            } else if ((symbol.isConst() || isGlobal())) {
                OpTree opTree = new OpTree(current, OpTree.OpType.number);
                opTree.setNumber(symbol.getNumber());
                current.addChild(opTree);
            } else {
                Value pointer = symbol.getValue();
//                Value value = new Load(pointer, curBasicBlock);
                OpTree opTree = new OpTree(current, OpTree.OpType.loadType);
                opTree.setValue(pointer);
                current.addChild(opTree);
            }
        } else {
//            ArrayList<Value> idxList = new ArrayList<>();
            Value pointer = symbol.getValue();
//            Type basicType = pointer.getType().getBasicType();
            OpTree opTree = new OpTree(current, OpTree.OpType.arrayType);
            opTree.setValue(pointer);
            current.addChild(opTree);
            current = opTree;
            for(int i = 0; i < ctx.exp().size(); i ++){
                visit(ctx.exp(i));
//                Value offset = OpTreeHandler.evalExp(current.getLast(), curBasicBlock);
//                offset = turnTo(offset, Int32Type.getInstance());
//                if(i == 0)
//                {
//                    if(basicType instanceof PointerType){
//                        basicType = basicType.getBasicType();
//                        pointer = new Load(pointer, curBasicBlock);
//                    }else{
//                        assert basicType instanceof ArrayType;
//                        basicType = basicType.getBasicType();
//                        idxList.add(CONST_0);
//                    }
//                    idxList.add(offset);
//                }else{
//                    basicType = basicType.getBasicType();
//                    idxList.add(offset);
//                }
            }
            opTree.setNeedPointer(needPointer);
            current = opTree.getParent();
            if(needPointer){
                return OpTreeHandler.evalArray(opTree, curBasicBlock);
            }

//            pointer = new GetElementPtr(basicType, pointer, idxList, curBasicBlock);
//            if(needPointer){
//                return pointer;
//            }
//            Value value;
//            if(basicType instanceof ArrayType){
//                idxList =  new ArrayList<>();
//                idxList.add(CONST_0);
//                idxList.add(CONST_0);
//                value = new GetElementPtr(basicType.getBasicType(), pointer, idxList, curBasicBlock);
//            }else {
//                value =  new Load(pointer, curBasicBlock);
//            }
//            opTree.setValue(value);
        }
        return null;
    }

    @Override
    public Value visitPrimaryExp(SysYParser.PrimaryExpContext ctx) {
        if (ctx.exp() != null) {
            visit(ctx.exp());
        } else if (ctx.lVal() != null) {
            visit(ctx.lVal());
        } else if (ctx.number() != null) {
            visit(ctx.number());
        }
        return null;
    }

    @Override
    public Value visitNumber(SysYParser.NumberContext ctx) {
        OpTree opTree = new OpTree(current, OpTree.OpType.number);
        if (ctx.FLOAT_CONST() != null) {
            opTree.setNumber(Float.parseFloat(ctx.FLOAT_CONST().getText()));
        } else if (ctx.DECIMAL_CONST() != null) {
            opTree.setNumber(Integer.parseInt(ctx.DECIMAL_CONST().getText()));
        } else if (ctx.OCTAL_CONST() != null) {
            if (ctx.OCTAL_CONST().getText().length() > 1)
                opTree.setNumber(Integer.parseInt(ctx.OCTAL_CONST().getText().substring(1), 8));
            else
                opTree.setNumber(Integer.parseInt("0"));
        } else if (ctx.HEX_CONST() != null) {
            opTree.setNumber(Integer.parseInt(ctx.HEX_CONST().getText().substring(2), 16));
        }
        current.addChild(opTree);
        return null;
    }

    @Override
    public Value visitUnaryExp(SysYParser.UnaryExpContext ctx) {
        if (ctx.primaryExp() != null) {
            visit(ctx.primaryExp());
        } else if (ctx.IDENT() != null) {
            String ident = ctx.IDENT().getText();
            Function function = Manager.getFunctions().get(ident);
            assert function != null;
            ArrayList<Value> params = new ArrayList<>();
            OpTree opTree = new OpTree(current, OpTree.OpType.funcType);
            opTree.setValue(function);
            current.addChild(opTree);
            current = opTree;
            if(ctx.funcRParams()!= null){
                visit(ctx.funcRParams());
            }
            current = current.getParent();
        } else {
            switch (ctx.unaryOp().getText()) {
                case "+" -> {
                    visit(ctx.unaryExp());
                }
                case "-" -> {
                    visitNegOrNotUnaryExp(ctx, OpTree.Operator.Neg);
                }
                case "!" -> {
                    visitNegOrNotUnaryExp(ctx, OpTree.Operator.Not);
                }
            }
        }
        return null;
    }

    public void visitNegOrNotUnaryExp(SysYParser.UnaryExpContext ctx, OpTree.Operator op) {
        OpTree opTree = new OpTree(new ArrayList<>(), new ArrayList<>(), current, OpTree.OpType.unaryType);
        opTree.appendOp(op);
        current.addChild(opTree);
        current = opTree;
        visit(ctx.unaryExp());
        if (current.getLast().getType().equals(OpTree.OpType.unaryType) && current.getLast().getOperators().get(0) == op) {
            current.getParent().removeLast();
            current.getChildren().get(0).getChildren().get(0).setParent(current.getParent());
            current.getParent().addChild(current.getChildren().get(0).getChildren().get(0));
        }
        current = current.getParent();
    }

    @Override
    public Value visitUnaryOp(SysYParser.UnaryOpContext ctx) {
        return null;
    }

    @Override
    public Value visitFuncRParams(SysYParser.FuncRParamsContext ctx) {
//        curFuncRParams = new ArrayList<>();
        for (int i = 0; i < ctx.exp().size(); i++) {
            visit(ctx.exp(i));
//            curFuncRParams.add(OpTreeHandler.evalExp(current.getLast(), curBasicBlock));
        }
        return null;
    }

    @Override
    public Value visitMulExp(SysYParser.MulExpContext ctx) {
        if (ctx.unaryExp().size() == 1) {
            visit(ctx.unaryExp(0));
        } else {
            OpTree opTree = new OpTree(new ArrayList<>(), new ArrayList<>(), current, OpTree.OpType.binaryType);
            current.addChild(opTree);
            current = opTree;
            for (int i = 0; i < ctx.unaryExp().size(); i++) {
                visit(ctx.unaryExp(i));
                if (i == 0)
                    continue;
                switch (ctx.mulOp(i - 1).getText()) {
                    case "*" -> {
                        opTree.appendOp(OpTree.Operator.Mul);
                    }
                    case "/" -> {
                        opTree.appendOp(OpTree.Operator.Div);
                    }
                    case "%" -> {
                        opTree.appendOp(OpTree.Operator.Mod);
                    }
                }
            }
            current = current.getParent();
        }
        return null;
    }

    @Override
    public Value visitMulOp(SysYParser.MulOpContext ctx) {
        return null;
    }

    @Override
    public Value visitAddExp(SysYParser.AddExpContext ctx) {
        if (ctx.mulExp().size() == 1) {
            visit(ctx.mulExp(0));
        } else {
            OpTree opTree = new OpTree(new ArrayList<>(), new ArrayList<>(), current, OpTree.OpType.binaryType);
            current.addChild(opTree);
            current = opTree;
            for (int i = 0; i < ctx.mulExp().size(); i++) {
                visit(ctx.mulExp(i));
                if (i == 0)
                    continue;
                switch (ctx.addOp(i - 1).getText()) {
                    case "+" -> {
                        opTree.appendOp(OpTree.Operator.Add);
                    }
                    case "-" -> {
                        opTree.appendOp(OpTree.Operator.Sub);
                    }
                }
            }
            current = current.getParent();
            // System.out.println(current.getLast().getLast().getNumberType());
            // System.out.println(Evaluate.evalConstExp(current.getLast()));
        }
        return null;
    }

    @Override
    public Value visitAddOp(SysYParser.AddOpContext ctx) {
        return null;
    }

    @Override
    public Value visitRelExp(SysYParser.RelExpContext ctx) {
        if (ctx.addExp().size() == 1) {
            visit(ctx.addExp(0));
        } else {
            OpTree opTree = new OpTree(new ArrayList<>(), new ArrayList<>(), current, OpTree.OpType.condType);
            current.addChild(opTree);
            current = opTree;
            for (int i = 0; i < ctx.addExp().size(); i++) {
                visit(ctx.addExp(i));
                if (i == 0)
                    continue;
                switch (ctx.relOp(i - 1).getText()) {
                    case "<" -> {
                        opTree.appendOp(OpTree.Operator.Lt);
                    }
                    case ">" -> {
                        opTree.appendOp(OpTree.Operator.Gt);
                    }
                    case "<=" -> {
                        opTree.appendOp(OpTree.Operator.Le);
                    }
                    case ">=" -> {
                        opTree.appendOp(OpTree.Operator.Ge);
                    }
                }
            }
            current = current.getParent();
        }
        return null;
    }

    @Override
    public Value visitRelOp(SysYParser.RelOpContext ctx) {
        return null;
    }

    @Override
    public Value visitEqExp(SysYParser.EqExpContext ctx) {
        if (ctx.relExp().size() == 1) {
            visit(ctx.relExp(0));
        } else {
            OpTree opTree = new OpTree(new ArrayList<>(), new ArrayList<>(), current, OpTree.OpType.condType);
            current.addChild(opTree);
            current = opTree;
            for (int i = 0; i < ctx.relExp().size(); i++) {
                visit(ctx.relExp(i));
                if (i == 0)
                    continue;
                switch (ctx.eqOp(i - 1).getText()) {
                    case "==" -> {
                        opTree.appendOp(OpTree.Operator.Eq);
                    }
                    case "!=" -> {
                        opTree.appendOp(OpTree.Operator.Ne);
                    }
                }
            }
            current = current.getParent();
        }
        return null;
    }

    @Override
    public Value visitEqOp(SysYParser.EqOpContext ctx) {
        return null;
    }

    @Override
    public Value visitLAndExp(SysYParser.LAndExpContext ctx) {
        if (ctx.eqExp().size() == 1) {
            visit(ctx.eqExp(0));
        } else {
            OpTree opTree = new OpTree(new ArrayList<>(), new ArrayList<>(), current, OpTree.OpType.condType);
            current.addChild(opTree);
            current = opTree;
            for (int i = 0; i < ctx.eqExp().size(); i++) {
                visit(ctx.eqExp(i));
                if (i == 0)
                    continue;
                opTree.appendOp(OpTree.Operator.And);
            }
            current = current.getParent();
        }
        return null;
    }

    @Override
    public Value visitLOrExp(SysYParser.LOrExpContext ctx) {
        if (ctx.lAndExp().size() == 1) {
            visit(ctx.lAndExp(0));
        } else {
            OpTree opTree = new OpTree(new ArrayList<>(), new ArrayList<>(), current, OpTree.OpType.condType);
            current.addChild(opTree);
            current = opTree;
            for (int i = 0; i < ctx.lAndExp().size(); i++) {
                visit(ctx.lAndExp(i));
                if (i == 0)
                    continue;
                opTree.appendOp(OpTree.Operator.Or);
            }
            current = current.getParent();
        }
        return null;
    }

    @Override
    public Value visitConstExp(SysYParser.ConstExpContext ctx) {
        visitChildren(ctx);
        return null;
    }
}
