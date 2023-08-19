package ir;

import ir.type.Type;
import util.MyList;
import util.MyNode;

import java.util.ArrayList;

public class Value extends MyNode {
    public Type type;
    public String name;
    public MyList<Used> usedInfo = new MyList<>(); //使用当前value的相关信息

    public Value(){

    }

    public Type getType() {
        return type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public MyList<Used> getUsedInfo() {
        return usedInfo;
    }

    public Value getUser(int i){
        return usedInfo.get(i).getUser();
    }

    public int getUsedSize(){
        return usedInfo.size();
    }

    public void addUsed(Used used){
        usedInfo.insertTail(used);
    }

    public void removeUser(Value user){
        for(int i = 0; i < usedInfo.size; i++){
            if(usedInfo.get(i).getUser().equals(user)){
                //System.out.println(user.getName()+" remove "+this.getName());
                usedInfo.remove(usedInfo.get(i));
                return;
            }
        }
    }

    /**
     * 对于所有用到自己的地方，用value替换自己
     */
    public void repalceUseofMeto(Value value){
        for(Used used:usedInfo){
            used.getUser().replaceUse(this,value);
        }
    }

    public Value getClone(){
        return this;
    }

}
