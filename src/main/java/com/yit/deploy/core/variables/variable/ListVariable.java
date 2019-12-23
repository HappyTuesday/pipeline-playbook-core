package com.yit.deploy.core.variables.variable;

import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.function.SubList;
import com.yit.deploy.core.model.VariableName;
import com.yit.deploy.core.variables.resolvers.ResolveContext;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Represents a list of variables.
 * The size of this list is saved as a *.size variable separately.
 * If no element of a specified index is found (but fails in the range of the valid indices), null will be returned when you fetch it.
 * But no null element will be returned when you are iterating this list by `foreach`, `iterator()` or `listIterator()`.
 *
 */
public abstract class ListVariable<T> extends ContextualVariable<List<T>, ListVariable<T>> implements List<T>, Cloneable {

    protected ListVariable(VariableName name, String id, ResolveContext context) {
        super(name, id, context);
    }

    /**
     * with the specified context
     *
     * @param context resolve context
     * @return new created context
     */
    @Override
    public abstract ListVariable<T> context(ResolveContext context);

    /**
     * get the variable of the specified index in this list
     * @param index index name
     * @return variable of the key
     */
    protected abstract Variable<T> variable(int index);

    /**
     * get all variables defined in this list
     * @return the iterator
     */
    @Nonnull
    protected abstract Iterator<Variable<T>> variables();

    /**
     * if the list variable works in lazy mode
     * @return return true if in lazy mode
     */
    protected abstract boolean lazyMode();

    /**
     * Creates and returns a copy of this object.
     *
     * @see Cloneable
     */
    @Override
    public ListVariable<T> clone() {
        return new SimpleListVariable<>(this, context);
    }

    /**
     * estimate the size of this list
     * @return size
     */
    protected abstract int estimateSize();

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
        return Lambda.contains(iterator(), o);
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
        if (index < 0) {
            throw new IndexOutOfBoundsException(String.valueOf(index));
        }

        Variable<T> v = variable(index);
        if (v == null) {
            throw new IndexOutOfBoundsException(String.valueOf(index));
        }

        return v.resolve(context);
    }

    /**
     * Returns an array containing all of the elements in this list in proper
     * sequence (from first to last element).
     *
     * <p>The returned array will be "safe" in that no references to it are
     * maintained by this list.  (In other words, this method must
     * allocate a new array even if this list is backed by an array).
     * The caller is thus free to modify the returned array.
     *
     * <p>This method acts as bridge between array-based and collection-based
     * APIs.
     *
     * @return an array containing all of the elements in this list in proper
     * sequence
     * @see Arrays#asList(Object[])
     */
    @Override
    public Object[] toArray() {
        return Lambda.asList(iterator()).toArray();
    }

    /**
     * Returns an array containing all of the elements in this list in
     * proper sequence (from first to last element); the runtime type of
     * the returned array is that of the specified array.  If the list fits
     * in the specified array, it is returned therein.  Otherwise, a new
     * array is allocated with the runtime type of the specified array and
     * the size of this list.
     *
     * <p>If the list fits in the specified array with room to spare (i.e.,
     * the array has more elements than the list), the element in the array
     * immediately following the end of the list is set to <tt>null</tt>.
     * (This is useful in determining the length of the list <i>only</i> if
     * the caller knows that the list does not contain any null elements.)
     *
     * <p>Like the {@link #toArray()} method, this method acts as bridge between
     * array-based and collection-based APIs.  Further, this method allows
     * precise control over the runtime type of the output array, and may,
     * under certain circumstances, be used to save allocation costs.
     *
     * <p>Suppose <tt>x</tt> is a list known to contain only strings.
     * The following code can be used to dump the list into a newly
     * allocated array of <tt>String</tt>:
     *
     * <pre>{@code
     *     String[] y = x.toArray(new String[0]);
     * }</pre>
     * <p>
     * Note that <tt>toArray(new Object[0])</tt> is identical in function to
     * <tt>toArray()</tt>.
     *
     * @param a the array into which the elements of this list are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose.
     * @return an array containing the elements of this list
     * @throws ArrayStoreException  if the runtime type of the specified array
     *                              is not a supertype of the runtime type of every element in
     *                              this list
     * @throws NullPointerException if the specified array is null
     */
    @Override
    public <T1> T1[] toArray(T1[] a) {
        return Lambda.asList(iterator()).toArray(a);
    }

    /**
     * Removes the first occurrence of the specified element from this list,
     * if it is present (optional operation).  If this list does not contain
     * the element, it is unchanged.  More formally, removes the element with
     * the lowest index <tt>i</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>
     * (if such an element exists).  Returns <tt>true</tt> if this list
     * contained the specified element (or equivalently, if this list changed
     * as a result of the call).
     *
     * @param o element to be removed from this list, if present
     * @return <tt>true</tt> if this list contained the specified element
     * @throws ClassCastException            if the type of the specified element
     *                                       is incompatible with this list
     *                                       (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException          if the specified element is null and this
     *                                       list does not permit null elements
     *                                       (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws UnsupportedOperationException if the <tt>remove</tt> operation
     *                                       is not supported by this list
     */
    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns <tt>true</tt> if this list contains all of the elements of the
     * specified collection.
     *
     * @param c collection to be checked for containment in this list
     * @return <tt>true</tt> if this list contains all of the elements of the
     * specified collection
     * @throws ClassCastException   if the types of one or more elements
     *                              in the specified collection are incompatible with this
     *                              list
     *                              (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified collection contains one
     *                              or more null elements and this list does not permit null
     *                              elements
     *                              (<a href="Collection.html#optional-restrictions">optional</a>),
     *                              or if the specified collection is null
     * @see #contains(Object)
     */
    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) return false;
        }
        return true;
    }

    /**
     * Appends all of the elements in the specified collection to the end of
     * this list, in the order that they are returned by the specified
     * collection's iterator (optional operation).  The behavior of this
     * operation is undefined if the specified collection is modified while
     * the operation is in progress.  (Note that this will occur if the
     * specified collection is this list, and it's nonempty.)
     *
     * @param c collection containing elements to be added to this list
     * @return <tt>true</tt> if this list changed as a result of the call
     * @throws UnsupportedOperationException if the <tt>addAll</tt> operation
     *                                       is not supported by this list
     * @throws ClassCastException            if the class of an element of the specified
     *                                       collection prevents it from being added to this list
     * @throws NullPointerException          if the specified collection contains one
     *                                       or more null elements and this list does not permit null
     *                                       elements, or if the specified collection is null
     * @throws IllegalArgumentException      if some property of an element of the
     *                                       specified collection prevents it from being added to this list
     * @see #add(Object)
     */
    @Override
    public boolean addAll(Collection<? extends T> c) {
        for (T o : c) {
            add(o);
        }
        return true;
    }

    /**
     * Inserts all of the elements in the specified collection into this
     * list at the specified position (optional operation).  Shifts the
     * element currently at that position (if any) and any subsequent
     * elements to the right (increases their indices).  The new elements
     * will appear in this list in the order that they are returned by the
     * specified collection's iterator.  The behavior of this operation is
     * undefined if the specified collection is modified while the
     * operation is in progress.  (Note that this will occur if the specified
     * collection is this list, and it's nonempty.)
     *
     * @param index index at which to insert the first element from the
     *              specified collection
     * @param c     collection containing elements to be added to this list
     * @return <tt>true</tt> if this list changed as a result of the call
     * @throws UnsupportedOperationException if the <tt>addAll</tt> operation
     *                                       is not supported by this list
     * @throws ClassCastException            if the class of an element of the specified
     *                                       collection prevents it from being added to this list
     * @throws NullPointerException          if the specified collection contains one
     *                                       or more null elements and this list does not permit null
     *                                       elements, or if the specified collection is null
     * @throws IllegalArgumentException      if some property of an element of the
     *                                       specified collection prevents it from being added to this list
     * @throws IndexOutOfBoundsException     if the index is out of range
     *                                       (<tt>index &lt; 0 || index &gt; size()</tt>)
     */
    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        throw new UnsupportedOperationException();
    }

    /**
     * Removes from this list all of its elements that are contained in the
     * specified collection (optional operation).
     *
     * @param c collection containing elements to be removed from this list
     * @return <tt>true</tt> if this list changed as a result of the call
     * @throws UnsupportedOperationException if the <tt>removeAll</tt> operation
     *                                       is not supported by this list
     * @throws ClassCastException            if the class of an element of this list
     *                                       is incompatible with the specified collection
     *                                       (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException          if this list contains a null element and the
     *                                       specified collection does not permit null elements
     *                                       (<a href="Collection.html#optional-restrictions">optional</a>),
     *                                       or if the specified collection is null
     * @see #remove(Object)
     * @see #contains(Object)
     */
    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    /**
     * Retains only the elements in this list that are contained in the
     * specified collection (optional operation).  In other words, removes
     * from this list all of its elements that are not contained in the
     * specified collection.
     *
     * @param c collection containing elements to be retained in this list
     * @return <tt>true</tt> if this list changed as a result of the call
     * @throws UnsupportedOperationException if the <tt>retainAll</tt> operation
     *                                       is not supported by this list
     * @throws ClassCastException            if the class of an element of this list
     *                                       is incompatible with the specified collection
     *                                       (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException          if this list contains a null element and the
     *                                       specified collection does not permit null elements
     *                                       (<a href="Collection.html#optional-restrictions">optional</a>),
     *                                       or if the specified collection is null
     * @see #remove(Object)
     * @see #contains(Object)
     */
    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    /**
     * Removes all of the elements from this list (optional operation).
     * The list will be empty after this call returns.
     *
     * @throws UnsupportedOperationException if the <tt>clear</tt> operation
     *                                       is not supported by this list
     */
    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * Inserts the specified element at the specified position in this list
     * (optional operation).  Shifts the element currently at that position
     * (if any) and any subsequent elements to the right (adds one to their
     * indices).
     *
     * @param index   index at which the specified element is to be inserted
     * @param element element to be inserted
     * @throws UnsupportedOperationException if the <tt>add</tt> operation
     *                                       is not supported by this list
     * @throws ClassCastException            if the class of the specified element
     *                                       prevents it from being added to this list
     * @throws NullPointerException          if the specified element is null and
     *                                       this list does not permit null elements
     * @throws IllegalArgumentException      if some property of the specified
     *                                       element prevents it from being added to this list
     * @throws IndexOutOfBoundsException     if the index is out of range
     *                                       (<tt>index &lt; 0 || index &gt; size()</tt>)
     */
    @Override
    public void add(int index, T element) {
        throw new UnsupportedOperationException();
    }

    /**
     * Removes the element at the specified position in this list (optional
     * operation).  Shifts any subsequent elements to the left (subtracts one
     * from their indices).  Returns the element that was removed from the
     * list.
     *
     * @param index the index of the element to be removed
     * @return the element previously at the specified position
     * @throws UnsupportedOperationException if the <tt>remove</tt> operation
     *                                       is not supported by this list
     * @throws IndexOutOfBoundsException     if the index is out of range
     *                                       (<tt>index &lt; 0 || index &gt;= size()</tt>)
     */
    @Override
    public T remove(int index) {
        throw new UnsupportedOperationException();
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
        int i = 0;
        for (Object e : this) {
            if (Objects.equals(e, o)) {
                return i;
            }
            i++;
        }
        return -1;
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
        int i = 0, j = -1;
        for (Object e : this) {
            if (Objects.equals(e, o)) {
                j = i;
            }
            i++;
        }
        return j;
    }

    /**
     * Returns an iterator over the elements in this list in proper sequence.
     *
     * @return an iterator over the elements in this list in proper sequence
     */
    @Nonnull
    @Override
    public Iterator<T> iterator() {
        return Lambda.map(variables(), v -> v.resolve(context));
    }

    /**
     * Returns a list iterator over the elements in this list (in proper
     * sequence).
     *
     * @return a list iterator over the elements in this list (in proper
     * sequence)
     */
    @Nonnull
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
    @Nonnull
    @Override
    public ListIterator<T> listIterator(int index) {

        return new ListIterator<T>() {

            int i = index - 1;
            int c = size();
            Variable<T> next, previous;

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
                if (next != null) {
                    return true;
                }

                while (++i < c) {
                    next = variable(i);
                    if (next != null) return true;
                }
                return false;
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
                if (next == null) {
                    throw new NoSuchElementException();
                }

                T o = next.resolve(context);
                next = null;
                return o;
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
                if (previous != null) {
                    return true;
                }

                while (--i >= 0) {
                    previous = variable(i);
                    if (previous != null) return true;
                }
                return false;
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
                if (previous == null) {
                    throw new NoSuchElementException();
                }

                T o = previous.resolve(context);
                previous = null;
                return o;
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
                return i;
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
                return i;
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
                ListVariable.this.set(i, t);
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

    /**
     * Returns a view of the portion of this list between the specified
     * <tt>fromIndex</tt>, inclusive, and <tt>toIndex</tt>, exclusive.  (If
     * <tt>fromIndex</tt> and <tt>toIndex</tt> are equal, the returned list is
     * empty.)  The returned list is backed by this list, so non-structural
     * changes in the returned list are reflected in this list, and vice-versa.
     * The returned list supports all of the optional list operations supported
     * by this list.<p>
     * <p>
     * This method eliminates the need for explicit range operations (of
     * the sort that commonly exist for arrays).  Any operation that expects
     * a list can be used as a range operation by passing a subList view
     * instead of a whole list.  For example, the following idiom
     * removes a range of elements from a list:
     * <pre>{@code
     *      list.subList(from, to).clear();
     * }</pre>
     * Similar idioms may be constructed for <tt>indexOf</tt> and
     * <tt>lastIndexOf</tt>, and all of the algorithms in the
     * <tt>Collections</tt> class can be applied to a subList.<p>
     * <p>
     * The semantics of the list returned by this method become undefined if
     * the backing list (i.e., this list) is <i>structurally modified</i> in
     * any way other than via the returned list.  (Structural modifications are
     * those that change the size of this list, or otherwise perturb it in such
     * a fashion that iterations in progress may yield incorrect results.)
     *
     * @param fromIndex low endpoint (inclusive) of the subList
     * @param toIndex   high endpoint (exclusive) of the subList
     * @return a view of the specified range within this list
     * @throws IndexOutOfBoundsException for an illegal endpoint index value
     *                                   (<tt>fromIndex &lt; 0 || toIndex &gt; size ||
     *                                   fromIndex &gt; toIndex</tt>)
     */
    @Nonnull
    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return new SubList<>(this, fromIndex, toIndex);
    }
}
