package frontend.parser;

import frontend.semantic.Evaluate;
import frontend.semantic.OpTree;
import frontend.semantic.symbol.SymTable;
import frontend.semantic.symbol.Symbol;
import ir.Value;
import ir.type.FloatType;
import ir.type.Int32Type;
import ir.type.Type;
import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;

import java.util.ArrayList;

public class Visitor extends AbstractParseTreeVisitor<Value> implements SysYVisitor<Value>{
    public static final Visitor Instance = new Visitor();
    private Visitor(){}

    private Type defContextType = null;
    private OpTree current = new OpTree(new ArrayList<>(),new ArrayList<>(),null, null);
    private SymTable curSymTable = new SymTable(null);

    @Override
    public Value visitCompUnit(SysYParser.CompUnitContext ctx) {
        System.out.println("visitCompUnit");
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
        //每一维数组的长度
        ArrayList<Integer> lengths = new ArrayList<>();
        for(var exp: ctx.constExp()){
            visit(exp);
            lengths.add((Integer) Evaluate.evalConstExp(current.getLast()));
        }

        return null;
    }

    @Override
    public Value visitConstInitVal(SysYParser.ConstInitValContext ctx) {
        return null;
    }

    @Override
    public Value visitVarDecl(SysYParser.VarDeclContext ctx) {
        return null;
    }

    @Override
    public Value visitVarDef(SysYParser.VarDefContext ctx) {
        return null;
    }

    @Override
    public Value visitInitVal(SysYParser.InitValContext ctx) {
        return null;
    }

    @Override
    public Value visitFuncDef(SysYParser.FuncDefContext ctx) {

        return null;
    }

    @Override
    public Value visitFuncType(SysYParser.FuncTypeContext ctx) {
        return null;
    }

    @Override
    public Value visitFuncFParams(SysYParser.FuncFParamsContext ctx) {
        return null;
    }

    @Override
    public Value visitFuncFParam(SysYParser.FuncFParamContext ctx) {
        return null;
    }

    @Override
    public Value visitBlock(SysYParser.BlockContext ctx) {
        return null;
    }

    @Override
    public Value visitBlockItem(SysYParser.BlockItemContext ctx) {
        return null;
    }

    @Override
    public Value visitStmt(SysYParser.StmtContext ctx) {
        return null;
    }

    @Override
    public Value visitExp(SysYParser.ExpContext ctx) {
        return null;
    }

    @Override
    public Value visitCond(SysYParser.CondContext ctx) {
        return null;
    }

    @Override
    public Value visitLVal(SysYParser.LValContext ctx) {
        String ident = ctx.IDENT().getText();
        Symbol symbol = curSymTable.get(ident, true);
        if(symbol.isConst() && ctx.exp() == null){
            OpTree opTree = new OpTree(current, OpTree.OpType.number);
            opTree.setNumber(symbol.getNumber());
        }
        return null;
    }

    @Override
    public Value visitPrimaryExp(SysYParser.PrimaryExpContext ctx) {
        if(ctx.exp() != null){
            visit(ctx.exp());
        }else if(ctx.lVal() != null){
            visit(ctx.lVal());
        }else if(ctx.number() != null){
            visit(ctx.number());
        }
        return null;
    }

    @Override
    public Value visitNumber(SysYParser.NumberContext ctx) {
        OpTree opTree = new OpTree(current, OpTree.OpType.number);
        if(ctx.FLOAT_CONST() != null){
            opTree.setNumber(Float.parseFloat(ctx.FLOAT_CONST().getText()));
        }else if(ctx.DECIMAL_CONST() != null){
            opTree.setNumber(Integer.parseInt(ctx.DECIMAL_CONST().getText()));
        }else if(ctx.OCTAL_CONST() != null){
            if(ctx.OCTAL_CONST().getText().length() >1)
                opTree.setNumber(Integer.parseInt(ctx.OCTAL_CONST().getText().substring(1), 8));
            else
                opTree.setNumber(Integer.parseInt("0"));
        }else if(ctx.HEX_CONST() != null){
            opTree.setNumber(Integer.parseInt(ctx.HEX_CONST().getText().substring(2), 16));
        }
        current.addChild(opTree);
        return null;
    }

    @Override
    public Value visitUnaryExp(SysYParser.UnaryExpContext ctx) {
        if(ctx.primaryExp() != null){
            visit(ctx.primaryExp());
        }else if(ctx.IDENT() != null){
            System.err.println("visitUnaryExp:尚未实现函数调用功能");
        }else{
            switch (ctx.unaryOp().getText()){
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

    public void visitNegOrNotUnaryExp(SysYParser.UnaryExpContext ctx, OpTree.Operator op){
        OpTree opTree = new OpTree(new ArrayList<>(),new ArrayList<>(), current, OpTree.OpType.unaryType);
        opTree.appendOp(op);
        current.addChild(opTree);
        current = opTree;
        visit(ctx.unaryExp());
        if(current.getOperators().size() != 0 && current.getOperators().get(0) == op){
            current.getParent().removeLast();
            current.getChildren().get(0).setParent(current.getParent());
            current.getParent().addChild(current.getChildren().get(0));
        }
        current = current.getParent();
    }

    @Override
    public Value visitUnaryOp(SysYParser.UnaryOpContext ctx) {
        return null;
    }

    @Override
    public Value visitFuncRParams(SysYParser.FuncRParamsContext ctx) {
        return null;
    }

    @Override
    public Value visitMulExp(SysYParser.MulExpContext ctx) {
        if(ctx.unaryExp().size() == 1){
            visit(ctx.unaryExp(0));
        }else{
            OpTree opTree = new OpTree(new ArrayList<>(),new ArrayList<>(),current, OpTree.OpType.binaryType);
            current.addChild(opTree);
            current = opTree;
            for(int i = 0; i < ctx.unaryExp().size(); i++){
                visit(ctx.unaryExp(i));
                if(i == 0)
                    continue;
                switch (ctx.mulOp(i-1).getText()) {
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
        if(ctx.mulExp().size() == 1){
            visit(ctx.mulExp(0));
        }else{
            OpTree opTree = new OpTree(new ArrayList<>(),new ArrayList<>(),current, OpTree.OpType.binaryType);
            current.addChild(opTree);
            current = opTree;
            for(int i = 0; i < ctx.mulExp().size(); i++){
                visit(ctx.mulExp(i));
                if(i == 0)
                    continue;
                switch (ctx.addOp(i-1).getText()) {
                    case "+" -> {
                        opTree.appendOp(OpTree.Operator.Add);
                    }
                    case "-" -> {
                        opTree.appendOp(OpTree.Operator.Sub);
                    }
                }
            }
            current = current.getParent();
//            System.out.println(current.getLast().getLast().getNumberType());
//            System.out.println(Evaluate.evalConstExp(current.getLast()));
        }
        return null;
    }

    @Override
    public Value visitAddOp(SysYParser.AddOpContext ctx) {
        return null;
    }

    @Override
    public Value visitRelExp(SysYParser.RelExpContext ctx) {
        return null;
    }

    @Override
    public Value visitRelOp(SysYParser.RelOpContext ctx) {
        return null;
    }

    @Override
    public Value visitEqExp(SysYParser.EqExpContext ctx) {
        return null;
    }

    @Override
    public Value visitEqOp(SysYParser.EqOpContext ctx) {
        return null;
    }

    @Override
    public Value visitLAndExp(SysYParser.LAndExpContext ctx) {
        return null;
    }

    @Override
    public Value visitLOrExp(SysYParser.LOrExpContext ctx) {
        return null;
    }

    @Override
    public Value visitConstExp(SysYParser.ConstExpContext ctx) {
        visitChildren(ctx);
        return null;
    }
}
