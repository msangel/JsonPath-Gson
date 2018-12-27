package net.sinistersky.j2ee.support.iterators;

import java.util.ArrayList;
import java.util.ListIterator;
import static net.sinistersky.j2ee.support.JsonPath2.getStackOffset;

public class ArrayListPeekableIterator<T> extends PeekableIterator<T>{

	private ListIterator<T> iter;

	public ArrayListPeekableIterator(ArrayList<T> in) {
		this.iter = in.listIterator();
	}
	
	public boolean hasNext() {
		boolean hasNext = iter.hasNext();
		return hasNext;
	}

	public T next() {
		return iter.next();
	}

	public void remove() {
		iter.remove();
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