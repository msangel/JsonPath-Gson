package net.sinistersky.j2ee.support.iterators;

import net.sinistersky.j2ee.support.Expression;
import net.sinistersky.j2ee.support.nodetypes.PathNode;

import com.google.gson.JsonElement;

/**
 * This iterator recursively iterates over the JSON tree and evaluates the appropriate
 * expression part for each node.
 * All nodes that match to the whole expression are accessible via iterator.
 * @author Vasyl Khrystiuk
 *
 */
public class ExecIterator extends PeekableIterator<JsonElement> {

    private final Expression expression;
    private final PeekableIterator<JsonElement> in;
    private final int filterPosition;
    private final JsonElement root;

    private PeekableIterator<JsonElement> current;
    private JsonElement next = null;
    private boolean isNextTaken = false;

    public ExecIterator(Expression expression, PeekableIterator<JsonElement> in,
            int filterPosition, JsonElement root) {
        this.expression = expression;
        this.in = in;
        this.filterPosition = filterPosition;
        this.root = root;
    }

    public boolean hasNext() {
        if (current!=null) { // if have current iterator - delegate checking to it
            return current.hasNext();
        }
        if (!isNextTaken) {
            this.next = takeNext();
            isNextTaken = true;
        }
        return next != null;
    }


    public JsonElement next(){
        if(isNextTaken){
            isNextTaken = false;
            return next;
        } else {
            return takeNext();
        }
    }

    /**
     * This function returns only next or null.
     * This function should not change {@link #isNextTaken} and {@link #next} fields.
     * @return next item in iteration.
     */
    private JsonElement takeNext(){
        if(current!=null){ // if here - current has least one item
            if(current.hasNext()){
                JsonElement next = current.next();
                if(!current.hasNext()){
                    current = null;
                }
                return next;
            } else {
                current = null;
            }
        }

        if(filterPosition>=this.expression.getNodes().size()){
            if(in.hasNext()){
                return in.next();
            } else {
                return null;
            }
        }

        PathNode pathNode = expression.getNodes().get(filterPosition);

        while (in.hasNext()){
            JsonElement next = in.next();
            JsonElement effectiveRoot = root == null ? next : root;
            PeekableIterator<JsonElement> filtered =
                    pathNode.filter(next, effectiveRoot); // current element children
            if(filtered.hasNext()){
                // cases:
                // 1) no items - skip this case and trying to get item from next iteration
                // 2) one item - return it
                // 3) few items - save 'current' iterator for accession other items in this iterator for the next time.
                ExecIterator iter = new ExecIterator(
                        this.expression, filtered, filterPosition + 1, effectiveRoot);
                if(iter.hasNext()){
                    JsonElement returned = iter.next();
                    if(iter.hasNext()){ // few items
                        current = iter;
                    }
                    return returned;
                }
            } // no items - else in.next() and once again till not get result or all list is ended
            //  move this all to 'hasNext' for keeping logic correct
        }
        return null; // no items at all
    }

    @Override
    public JsonElement peek() {
        if(current!=null){
            return current.peek();
        }
        if(!isNextTaken){
            this.next = takeNext();
        }
        return next;
    }
}
