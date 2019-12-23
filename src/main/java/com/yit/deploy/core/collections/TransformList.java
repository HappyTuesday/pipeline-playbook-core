package com.yit.deploy.core.collections;

import java.util.*;
import java.util.function.Function;

public class TransformList<S, T> extends AbstractList<T> {

    private final List<S> list;
    private final Function<S, T> transformer;

    public TransformList(List<S> list, Function<S, T> transformer) {
        this.list = list;
        this.transformer = transformer;
    }

    /**
     * {@inheritDoc}
     *
     * @param index
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public T get(int index) {
        return transformer.apply(list.get(index));
    }

    @Override
    public int size() {
        return list.size();
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
        return new Iterator<T>() {

            Iterator<S> iter = list.iterator();

            /**
             * Returns {@code true} if the iteration has more elements.
             * (In other words, returns {@code true} if {@link #next} would
             * return an element rather than throwing an exception.)
             *
             * @return {@code true} if the iteration has more elements
             */
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            /**
             * Returns the next element in the iteration.
             *
             * @return the next element in the iteration
             * @throws NoSuchElementException if the iteration has no more elements
             */
            @Override
            public T next() {
                return transformer.apply(iter.next());
            }
        };
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation returns a straightforward implementation of the
     * {@code ListIterator} interface that extends the implementation of the
     * {@code Iterator} interface returned by the {@code iterator()} method.
     * The {@code ListIterator} implementation relies on the backing list's
     * {@code get(int)}, {@code set(int, E)}, {@code add(int, E)}
     * and {@code remove(int)} methods.
     *
     * <p>Note that the list iterator returned by this implementation will
     * throw an {@link UnsupportedOperationException} in response to its
     * {@code remove}, {@code set} and {@code add} methods unless the
     * list's {@code remove(int)}, {@code set(int, E)}, and
     * {@code add(int, E)} methods are overridden.
     *
     * <p>This implementation can be made to throw runtime exceptions in the
     * face of concurrent modification, as described in the specification for
     * the (protected) {@link #modCount} field.
     *
     * @param index
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public ListIterator<T> listIterator(int index) {
        return new ListIterator<T>() {

            ListIterator<S> iter = list.listIterator(index);

            /**
             * Returns {@code true} if this list iterator has more elements when
             * traversing the list in the forward direction. (In other words,
             * returns {@code true} if {@link #next} would return an element rather
             * than throwing an exception.)
             *
             * @return {@code true} if the list iterator has more elements when
             * traversing the list in the forward direction
             */
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            /**
             * Returns the next element in the list and advances the cursor position.
             * This method may be called repeatedly to iterate through the list,
             * or intermixed with calls to {@link #previous} to go back and forth.
             * (Note that alternating calls to {@code next} and {@code previous}
             * will return the same element repeatedly.)
             *
             * @return the next element in the list
             * @throws NoSuchElementException if the iteration has no next element
             */
            @Override
            public T next() {
                return transformer.apply(iter.next());
            }

            /**
             * Returns {@code true} if this list iterator has more elements when
             * traversing the list in the reverse direction.  (In other words,
             * returns {@code true} if {@link #previous} would return an element
             * rather than throwing an exception.)
             *
             * @return {@code true} if the list iterator has more elements when
             * traversing the list in the reverse direction
             */
            @Override
            public boolean hasPrevious() {
                return iter.hasPrevious();
            }

            /**
             * Returns the previous element in the list and moves the cursor
             * position backwards.  This method may be called repeatedly to
             * iterate through the list backwards, or intermixed with calls to
             * {@link #next} to go back and forth.  (Note that alternating calls
             * to {@code next} and {@code previous} will return the same
             * element repeatedly.)
             *
             * @return the previous element in the list
             * @throws NoSuchElementException if the iteration has no previous
             *                                element
             */
            @Override
            public T previous() {
                return transformer.apply(iter.previous());
            }

            /**
             * Returns the index of the element that would be returned by a
             * subsequent call to {@link #next}. (Returns list size if the list
             * iterator is at the end of the list.)
             *
             * @return the index of the element that would be returned by a
             * subsequent call to {@code next}, or list size if the list
             * iterator is at the end of the list
             */
            @Override
            public int nextIndex() {
                return iter.nextIndex();
            }

            /**
             * Returns the index of the element that would be returned by a
             * subsequent call to {@link #previous}. (Returns -1 if the list
             * iterator is at the beginning of the list.)
             *
             * @return the index of the element that would be returned by a
             * subsequent call to {@code previous}, or -1 if the list
             * iterator is at the beginning of the list
             */
            @Override
            public int previousIndex() {
                return iter.previousIndex();
            }

            /**
             * Removes from the list the last element that was returned by {@link
             * #next} or {@link #previous} (optional operation).  This call can
             * only be made once per call to {@code next} or {@code previous}.
             * It can be made only if {@link #add} has not been
             * called after the last call to {@code next} or {@code previous}.
             *
             * @throws UnsupportedOperationException if the {@code remove}
             *                                       operation is not supported by this list iterator
             * @throws IllegalStateException         if neither {@code next} nor
             *                                       {@code previous} have been called, or {@code remove} or
             *                                       {@code add} have been called after the last call to
             *                                       {@code next} or {@code previous}
             */
            @Override
            public void remove() {
                iter.remove();
            }

            /**
             * Replaces the last element returned by {@link #next} or
             * {@link #previous} with the specified element (optional operation).
             * This call can be made only if neither {@link #remove} nor {@link
             * #add} have been called after the last call to {@code next} or
             * {@code previous}.
             *
             * @param t the element with which to replace the last element returned by
             *          {@code next} or {@code previous}
             * @throws UnsupportedOperationException if the {@code set} operation
             *                                       is not supported by this list iterator
             * @throws ClassCastException            if the class of the specified element
             *                                       prevents it from being added to this list
             * @throws IllegalArgumentException      if some aspect of the specified
             *                                       element prevents it from being added to this list
             * @throws IllegalStateException         if neither {@code next} nor
             *                                       {@code previous} have been called, or {@code remove} or
             *                                       {@code add} have been called after the last call to
             *                                       {@code next} or {@code previous}
             */
            @Override
            public void set(T t) {
                throw new UnsupportedOperationException();
            }

            /**
             * Inserts the specified element into the list (optional operation).
             * The element is inserted immediately before the element that
             * would be returned by {@link #next}, if any, and after the element
             * that would be returned by {@link #previous}, if any.  (If the
             * list contains no elements, the new element becomes the sole element
             * on the list.)  The new element is inserted before the implicit
             * cursor: a subsequent call to {@code next} would be unaffected, and a
             * subsequent call to {@code previous} would return the new element.
             * (This call increases by one the value that would be returned by a
             * call to {@code nextIndex} or {@code previousIndex}.)
             *
             * @param t the element to insert
             * @throws UnsupportedOperationException if the {@code add} method is
             *                                       not supported by this list iterator
             * @throws ClassCastException            if the class of the specified element
             *                                       prevents it from being added to this list
             * @throws IllegalArgumentException      if some aspect of this element
             *                                       prevents it from being added to this list
             */
            @Override
            public void add(T t) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
