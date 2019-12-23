package com.yit.deploy.core.collections;

import java.util.*;

public class UnionList<T> extends AbstractList<T> {
    private final List<T> left, right;

    public UnionList(List<T> left, List<T> right) {
        this.left = left;
        this.right = right;
    }

    /**
     * Returns the number of elements in this list.  If this list contains
     * more than <tt>Integer.MAX_VALUE</tt> elements, returns
     * <tt>Integer.MAX_VALUE</tt>.
     *
     * @return the number of elements in this list
     */
    @Override
    public int size() {
        return left.size() + right.size();
    }

    /**
     * Returns <tt>true</tt> if this list contains the specified element.
     * More formally, returns <tt>true</tt> if and only if this list contains
     * at least one element <tt>e</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
     *
     * @param o element whose presence in this list is to be tested
     * @return <tt>true</tt> if this list contains the specified element
     * @throws ClassCastException   if the type of the specified element
     *                              is incompatible with this list
     *                              (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified element is null and this
     *                              list does not permit null elements
     *                              (<a href="Collection.html#optional-restrictions">optional</a>)
     */
    @Override
    public boolean contains(Object o) {
        return left.contains(o) || right.contains(o);
    }

    /**
     * Returns an iterator over the elements in this list in proper sequence.
     *
     * @return an iterator over the elements in this list in proper sequence
     */
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {

            Iterator<T> iter = left.iterator();
            boolean first = true;

            /**
             * Returns {@code true} if the iteration has more elements.
             * (In other words, returns {@code true} if {@link #next} would
             * return an element rather than throwing an exception.)
             *
             * @return {@code true} if the iteration has more elements
             */
            @Override
            public boolean hasNext() {
                if (iter.hasNext()) {
                    return true;
                } else if (first) {
                    iter = right.iterator();
                    first = false;
                    return iter.hasNext();
                } else {
                    return false;
                }
            }

            /**
             * Returns the next element in the iteration.
             *
             * @return the next element in the iteration
             * @throws NoSuchElementException if the iteration has no more elements
             */
            @Override
            public T next() {
                return iter.next();
            }
        };
    }

    /**
     * Returns the element at the specified position in this list.
     *
     * @param index index of the element to return
     * @return the element at the specified position in this list
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   (<tt>index &lt; 0 || index &gt;= size()</tt>)
     */
    @Override
    public T get(int index) {
        return index < left.size() ? left.get(index) : right.get(index - left.size());
    }

    /**
     * Returns the index of the first occurrence of the specified element
     * in this list, or -1 if this list does not contain the element.
     * More formally, returns the lowest index <tt>i</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>,
     * or -1 if there is no such index.
     *
     * @param o element to search for
     * @return the index of the first occurrence of the specified element in
     * this list, or -1 if this list does not contain the element
     * @throws ClassCastException   if the type of the specified element
     *                              is incompatible with this list
     *                              (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified element is null and this
     *                              list does not permit null elements
     *                              (<a href="Collection.html#optional-restrictions">optional</a>)
     */
    @Override
    public int indexOf(Object o) {
        int i = left.indexOf(o);
        if (i < 0) {
            i = right.indexOf(o);
            if (i >= 0) {
                i += left.size();
            }
        }
        return i;
    }

    /**
     * Returns the index of the last occurrence of the specified element
     * in this list, or -1 if this list does not contain the element.
     * More formally, returns the highest index <tt>i</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>,
     * or -1 if there is no such index.
     *
     * @param o element to search for
     * @return the index of the last occurrence of the specified element in
     * this list, or -1 if this list does not contain the element
     * @throws ClassCastException   if the type of the specified element
     *                              is incompatible with this list
     *                              (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified element is null and this
     *                              list does not permit null elements
     *                              (<a href="Collection.html#optional-restrictions">optional</a>)
     */
    @Override
    public int lastIndexOf(Object o) {
        int i = right.indexOf(o);
        if (i < 0) {
            i = left.indexOf(o);
        } else {
            i += left.size();
        }
        return i;
    }

    /**
     * Returns a list iterator over the elements in this list (in proper
     * sequence).
     *
     * @return a list iterator over the elements in this list (in proper
     * sequence)
     */
    @Override
    public ListIterator<T> listIterator() {
        return listIterator(0);
    }

    /**
     * Returns a list iterator over the elements in this list (in proper
     * sequence), starting at the specified position in the list.
     * The specified index indicates the first element that would be
     * returned by an initial call to {@link ListIterator#next next}.
     * An initial call to {@link ListIterator#previous previous} would
     * return the element with the specified index minus one.
     *
     * @param index index of the first element to be returned from the
     *              list iterator (by a call to {@link ListIterator#next next})
     * @return a list iterator over the elements in this list (in proper
     * sequence), starting at the specified position in the list
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   ({@code index < 0 || index > size()})
     */
    @Override
    public ListIterator<T> listIterator(int index) {
        return new ListIterator<T>() {

            int n = left.size();
            boolean first = index < n;
            ListIterator<T> iter = first ? left.listIterator(index) : right.listIterator(index - n);

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
                if (iter.hasNext()) {
                    return true;
                } else if (first) {
                    iter = right.listIterator();
                    first = false;
                    return iter.hasNext();
                } else {
                    return false;
                }
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
                return iter.next();
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
                if (iter.hasPrevious()) {
                    return true;
                } else if (first) {
                    return false;
                } else {
                    iter = left.listIterator(n - 1);
                    first = true;
                    return iter.hasPrevious();
                }
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
                return iter.previous();
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
                return first ? iter.nextIndex() : iter.nextIndex() + n;
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
                return first ? iter.previousIndex() : iter.previousIndex() + n;
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
                throw new UnsupportedOperationException();
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
