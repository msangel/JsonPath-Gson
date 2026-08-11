package net.sinistersky.j2ee.support.iterators;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OneItemIterator<T> extends PeekableIterator<T>{

    private final T element;
    boolean isTaken = false;

    public boolean hasNext() {
        return !isTaken;
    }

    public T next() {
        if(!isTaken){
            isTaken = true;
            return element;
        } else {
            return null;
        }
    }

    @Override
    public T peek() {
        if(!isTaken){
            return element;
        } else {
            return null;
        }
    }

}
