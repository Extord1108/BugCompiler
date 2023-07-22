package util;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * 双端链表
 */
public class MyList<E extends MyNode> implements Iterable<E> {

    public MyNode head;
    public MyNode tail;
    public int size = 0;

    public  MyList(){
        head = new MyNode();
        tail = new MyNode();
        head.setNext(tail);
        tail.setPrev(head);
        size = 0;
    }

    public void clear(){
        head.setNext(tail);
        tail.setPrev(head);
        size = 0;
    }

    public E getFirst(){
        assert head.getNext() != tail;
        return (E) head.getNext();
    }

    public E getLast(){
        assert tail.getPrev() != head;
        return (E) tail.getPrev();
    }

    public void insertHead(E node){
        node.setNext(head.getNext());
        node.setPrev(head);
        head.getNext().setPrev(node);
        head.setNext(node);
        size ++;
    }

    public void insertTail(E node){
        node.setNext(tail);
        node.setPrev(tail.getPrev());
        tail.getPrev().setNext(node);
        tail.setPrev(node);
        size ++;
    }

    public void insertAfter(E p, E node){
            node.setNext(p.getNext());
            node.setPrev(p);
            p.getNext().setPrev(node);
            p.setNext(node);
            size ++;
    }

    public void insertBefore(E p, E node){
        node.setNext(p);
        node.setPrev(p.getPrev());
        p.getPrev().setNext(node);
        p.setPrev(node);
        size ++;
    }

    @Override
    public Iterator<E> iterator() {
        return new MyIterator();
    }

    class MyIterator implements Iterator<E>{

        MyNode cur = head;
        @Override
        public boolean hasNext() {
            return cur.getNext() != tail;
        }

        @Override
        public E next() {
            cur = cur.getNext();
            return (E) cur;
        }

        @Override
        public void remove(){
            cur.getPrev().setNext(cur.getNext());
            cur.getNext().setPrev(cur.getPrev());
            size --;
        }
    }
}
