package util;

public class MyNode {
    private MyNode next;
    private MyNode prev;

    public MyNode getPrev(){
        return this.prev;
    }

    public void setPrev(MyNode prev){
        this.prev = prev;
    }

    public MyNode getNext(){
        return this.next;
    }

    public void setNext(MyNode next){
        this.next = next;
    }

}
