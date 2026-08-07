package net.sinistersky.j2ee.support.iterators;

import java.util.ArrayList;
import java.util.ListIterator;

public class ArrayListPeekableIterator<T> extends PeekableIterator<T>{

    private final ListIterator<T> iter;

    public ArrayListPeekableIterator(ArrayList<T> in) {
        this.iter = in.listIterator();
    }

    public boolean hasNext() {
        return iter.hasNext();
    }

    public T next() {
        return iter.next();
    }

    /**
     * Take next item but not shift position.
     */
    public T peek() {
        if(hasNext()){
            T res = iter.next();
            iter.previous();// return back
            return res;
        } else {
            return null;
        }
    }
}
