package ir;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * 记录循环信息
 */
public class Loop extends Value {
    public static final Loop rootLoop = new Loop();
    private static int loo_num = 0;

    private int label;
    private int depth=-1;

    private Loop parent;
    private Function function;
    private BasicBlock basicBlock;


    private ArrayList<Loop> children = new ArrayList<>();

    private HashSet<BasicBlock> basicBlocks = new HashSet<>();

    private BasicBlock header;

    public Loop(){
        label = -1;
    }

    public Loop(Loop parentLoop){
        this.label = loo_num++;
        this.parent = parentLoop;
        this.depth = parentLoop.depth+1;
        parentLoop.children.add(this);
        this.name = parentLoop.getName() + "-" + (depth==0?0:parentLoop.getChildren().size());
    }

    public ArrayList<Loop> getChildren() {
        return children;
    }

    public void setFunction(Function function){
        this.function = function;
    }

    public  Loop getParent(){
        return parent;
    }

    public void addBasicBlock(BasicBlock basicBlock){
        basicBlocks.add(basicBlock);
    }

    public void setHeader(BasicBlock basicBlock){
        this.header = basicBlock;
    }

    public int getDepth(){
        int depth = 0;
        Loop loop = this;
        while (loop.getParent()!=rootLoop){
            loop = loop.getParent();
            depth++;
        }
        return depth;
    }
}
