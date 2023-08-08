package ir;

/**
 * 记录循环信息
 */
public class Loop {
    private BasicBlock header;
    private BasicBlock latch;
    private BasicBlock preHeader;
    private BasicBlock exitBlock;

    public Loop(BasicBlock header, BasicBlock latch, BasicBlock preHeader, BasicBlock exitBlock) {
        this.header = header;
        this.latch = latch;
        this.preHeader = preHeader;
        this.exitBlock = exitBlock;
    }

    public BasicBlock getHeader() {
        return header;
    }

    public BasicBlock getLatch() {
        return latch;
    }

    public BasicBlock getPreHeader() {
        return preHeader;
    }

    public BasicBlock getExitBlock() {
        return exitBlock;
    }
}
