package com.yit.deploy.core.function;

import java.util.*;

public class SubList<T> extends AbstractList<T> {

    private List<T> list;
    private int fromIndex, toIndex;

    public SubList(List<T> list, int fromIndex, int toIndex) {
        this.list = list;
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
    }

    /**
     * Returns an iterator over the elements in this list in proper sequence.
     *
     * <p>This implementation returns a straightforward implementation of the
     * iterator interface, relying on the backing list's {@code size()},
     * {@code get(int)}, and {@code remove(int)} methods.
     *
     * <p>Note that the iterator returned by this method will throw an
     * {@link UnsupportedOperationException} in response to its
     * {@code remove} method unless the list's {@code remove(int)} method is
     * overridden.
     *
     * <p>This implementation can be made to throw runtime exceptions in the
     * face of concurrent modification, as described in the specification
     * for the (protected) {@link #modCount} field.
     *
     * @return an iterator over the elements in this list in proper sequence
     */
    @Override
    public Iterator<T> iterator() {

        Iterator<T> iter = list.iterator();
        for (int i = 0; i < fromIndex; i++) {
            if (iter.hasNext()) {
                iter.next();
            } else {
                return Lambda.emptyIterator();
            }
        }

        return new Iterator<T>() {

            int c = toIndex - fromIndex;

            /**
             * Returns {@code true} if the iteration has more elements.
             * (In other words, returns {@code true} if {@link #next} would
             * return an element rather than throwing an exception.)
             *
             * @return {@code true} if the iteration has more elements
             */
            @Override
            public boolean hasNext() {
                return c > 0 && iter.hasNext();
            }

            /**
             * Returns the next element in the iteration.
             *
             * @return the next element in the iteration
             * @throws NoSuchElementException if the iteration has no more elements
             */
            @Override
            public T next() {
                if (--c < 0) {
                    throw new NoSuchElementException();
                }
                return iter.next();
            }
        };
    }

    /**
     * {@inheritDoc}
     *
     * @param index
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public T get(int index) {
        return list.get(index - fromIndex);
    }

    @Override
    public int size() {
        int c = list.size();
        return Math.min(c, toIndex) - fromIndex;
    }
}
