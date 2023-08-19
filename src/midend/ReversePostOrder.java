package midend;

import ir.BasicBlock;
import ir.Function;
import util.MyList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ReversePostOrder {
    private Function function;
    private Integer dfsOrder = 0;
    private ArrayList<BasicBlock> reversePostOrderBB = new ArrayList<>();
    public ReversePostOrder(Function function) {
        this.function = function;
    }
    public ArrayList<BasicBlock> get(){
        MyList<BasicBlock> basicBlocks = function.getBasicBlocks();
        //对CFG图进行逆后序遍历
        HashMap<BasicBlock,Integer> visited = new HashMap<>();
        dfsOrder = basicBlocks.size();
        dfs(basicBlocks.get(0), visited);
        //对visited按照value进行排序
        ArrayList<Map.Entry<BasicBlock,Integer>> list = new ArrayList<>(visited.entrySet());
        list.sort((o1, o2) -> (o1.getValue() - o2.getValue()));
        for(Map.Entry<BasicBlock,Integer> entry : list){
            reversePostOrderBB.add(entry.getKey());
        }
        return reversePostOrderBB;
    }

    private void dfs(BasicBlock bb, HashMap<BasicBlock,Integer> visited){
        //System.out.println(bb.getName()+" "+bb.getInstrs().size());
        visited.put(bb, -1);
        for(BasicBlock successor : bb.getSuccessors()){
            if(visited.get(successor) == null){
                dfs(successor, visited);
            }
        }
        visited.put(bb, dfsOrder--);
    }
}
