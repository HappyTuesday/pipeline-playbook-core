package com.yit.deploy.core.variables.variable;

import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.model.VariableName;
import com.yit.deploy.core.variables.resolvers.ResolveContext;

import javax.annotation.Nonnull;
import java.util.*;

public abstract class MapVariable<T> extends ContextualVariable<Map<String, T>, MapVariable<T>> implements Map<String, T>, Cloneable {

    protected MapVariable(VariableName name, String id, ResolveContext context) {
        super(name, id, context);
    }

    /**
     * with the specified context
     *
     * @param context resolve context
     * @return new created context
     */
    @Override
    public abstract MapVariable<T> context(ResolveContext context);

    /**
     * get the variable of the specified key in this map
     * @param key key name
     * @return variable of the key
     */
    protected abstract Variable<T> variable(String key);

    /**
     * get all variables defined in this map
     * @return the iterator
     */
    @Nonnull
    protected abstract Iterator<Entry<String, Variable<T>>> variables();

    /**
     * if the map variable works in lazy mode
     * @return return true if in lazy mode
     */
    protected abstract boolean lazyMode();

    /**
     * estimate the size of this map
     * @return size
     */
    protected abstract int estimateSize();

    /**
     * clone this variable
     * @return the cloned instance
     */
    public MapVariable<T> clone() {
        return new SimpleMapVariable<>(this, context);
    }

    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the
     * specified value.  More formally, returns <tt>true</tt> if and only if
     * this map contains at least one mapping to a value <tt>v</tt> such that
     * <tt>(value==null ? v==null : value.equals(v))</tt>.  This operation
     * will probably require time linear in the map size for most
     * implementations of the <tt>Map</tt> interface.
     *
     * @param value value whose presence in this map is to be tested
     * @return <tt>true</tt> if this map maps one or more keys to the
     * specified value
     * @throws ClassCastException   if the value is of an inappropriate type for
     *                              this map
     *                              (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified value is null and this
     *                              map does not permit null values
     *                              (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     */
    @Override
    public boolean containsValue(Object value) {
        return Lambda.contains(values().iterator(), value);
    }

    /**
     * Removes the mapping for a key from this map if it is present
     * (optional operation).   More formally, if this map contains a mapping
     * from key <tt>k</tt> to value <tt>v</tt> such that
     * <code>(key==null ?  k==null : key.equals(k))</code>, that mapping
     * is removed.  (The map can contain at most one such mapping.)
     *
     * <p>Returns the value to which this map previously associated the key,
     * or <tt>null</tt> if the map contained no mapping for the key.
     *
     * <p>If this map permits null values, then a return value of
     * <tt>null</tt> does not <i>necessarily</i> indicate that the map
     * contained no mapping for the key; it's also possible that the map
     * explicitly mapped the key to <tt>null</tt>.
     *
     * <p>The map will not contain a mapping for the specified key once the
     * call returns.
     *
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with <tt>key</tt>, or
     * <tt>null</tt> if there was no mapping for <tt>key</tt>.
     * @throws UnsupportedOperationException if the <tt>remove</tt> operation
     *                                       is not supported by this map
     * @throws ClassCastException            if the key is of an inappropriate type for
     *                                       this map
     *                                       (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException          if the specified key is null and this
     *                                       map does not permit null keys
     *                                       (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     */
    @Override
    public T remove(Object key) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that {@code (key==null ? k==null :
     * key.equals(k))}, then this method returns {@code v}; otherwise
     * it returns {@code null}.  (There can be at most one such mapping.)
     *
     * <p>If this map permits null values, then a return value of
     * {@code null} does not <i>necessarily</i> indicate that the map
     * contains no mapping for the key; it's also possible that the map
     * explicitly maps the key to {@code null}.  The {@link #containsKey
     * containsKey} operation may be used to distinguish these two cases.
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or
     * {@code null} if this map contains no mapping for the key
     * @throws ClassCastException   if the key is of an inappropriate type for
     *                              this map
     *                              (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified key is null and this map
     *                              does not permit null keys
     *                              (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     */
    @Override
    public T get(Object key) {
        Variable<T> v = variable((String) key);
        return v == null ? null : v.resolve(context());
    }

    /**
     * Copies all of the mappings from the specified map to this map
     * (optional operation).  The effect of this call is equivalent to that
     * of calling {@link #put(Object, Object) put(k, v)} on this map once
     * for each mapping from key <tt>k</tt> to value <tt>v</tt> in the
     * specified map.  The behavior of this operation is undefined if the
     * specified map is modified while the operation is in progress.
     *
     * @param m mappings to be stored in this map
     * @throws UnsupportedOperationException if the <tt>putAll</tt> operation
     *                                       is not supported by this map
     * @throws ClassCastException            if the class of a key or value in the
     *                                       specified map prevents it from being stored in this map
     * @throws NullPointerException          if the specified map is null, or if
     *                                       this map does not permit null keys or values, and the
     *                                       specified map contains null keys or values
     * @throws IllegalArgumentException      if some property of a key or value in
     *                                       the specified map prevents it from being stored in this map
     */
    @Override
    public void putAll(Map<? extends String, ? extends T> m) {
        for (Entry<? extends String, ? extends T> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Removes all of the mappings from this map (optional operation).
     * The map will be empty after this call returns.
     *
     * @throws UnsupportedOperationException if the <tt>clear</tt> operation
     *                                       is not supported by this map
     */
    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a {@link Set} view of the keys contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own <tt>remove</tt> operation), the results of
     * the iteration are undefined.  The set supports element removal,
     * which removes the corresponding mapping from the map, via the
     * <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt>
     * operations.  It does not support the <tt>add</tt> or <tt>addAll</tt>
     * operations.
     *
     * @return a set view of the keys contained in this map
     */
    @Override
    public Set<String> keySet() {
        return new Set<String>() {
            /**
             * Returns the number of elements in this set (its cardinality).  If this
             * set contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
             * <tt>Integer.MAX_VALUE</tt>.
             *
             * @return the number of elements in this set (its cardinality)
             */
            @Override
            public int size() {
                return MapVariable.this.size();
            }

            /**
             * Returns <tt>true</tt> if this set contains no elements.
             *
             * @return <tt>true</tt> if this set contains no elements
             */
            @Override
            public boolean isEmpty() {
                return MapVariable.this.isEmpty();
            }

            /**
             * Returns <tt>true</tt> if this set contains the specified element.
             * More formally, returns <tt>true</tt> if and only if this set
             * contains an element <tt>e</tt> such that
             * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
             *
             * @param o element whose presence in this set is to be tested
             * @return <tt>true</tt> if this set contains the specified element
             * @throws ClassCastException   if the type of the specified element
             *                              is incompatible with this set
             *                              (<a href="Collection.html#optional-restrictions">optional</a>)
             * @throws NullPointerException if the specified element is null and this
             *                              set does not permit null elements
             *                              (<a href="Collection.html#optional-restrictions">optional</a>)
             */
            @Override
            public boolean contains(Object o) {
                return MapVariable.this.containsKey(o);
            }

            /**
             * Returns an iterator over the elements in this set.  The elements are
             * returned in no particular order (unless this set is an instance of some
             * class that provides a guarantee).
             *
             * @return an iterator over the elements in this set
             */
            @Override
            public Iterator<String> iterator() {
                return Lambda.map(MapVariable.this.variables(), Entry::getKey);
            }

            /**
             * Returns an array containing all of the elements in this set.
             * If this set makes any guarantees as to what order its elements
             * are returned by its iterator, this method must return the
             * elements in the same order.
             *
             * <p>The returned array will be "safe" in that no references to it
             * are maintained by this set.  (In other words, this method must
             * allocate a new array even if this set is backed by an array).
             * The caller is thus free to modify the returned array.
             *
             * <p>This method acts as bridge between array-based and collection-based
             * APIs.
             *
             * @return an array containing all the elements in this set
             */
            @Override
            public Object[] toArray() {
                return Lambda.asList(iterator()).toArray();
            }

            /**
             * Returns an array containing all of the elements in this set; the
             * runtime type of the returned array is that of the specified array.
             * If the set fits in the specified array, it is returned therein.
             * Otherwise, a new array is allocated with the runtime type of the
             * specified array and the size of this set.
             *
             * <p>If this set fits in the specified array with room to spare
             * (i.e., the array has more elements than this set), the element in
             * the array immediately following the end of the set is set to
             * <tt>null</tt>.  (This is useful in determining the length of this
             * set <i>only</i> if the caller knows that this set does not contain
             * any null elements.)
             *
             * <p>If this set makes any guarantees as to what order its elements
             * are returned by its iterator, this method must return the elements
             * in the same order.
             *
             * <p>Like the {@link #toArray()} method, this method acts as bridge between
             * array-based and collection-based APIs.  Further, this method allows
             * precise control over the runtime type of the output array, and may,
             * under certain circumstances, be used to save allocation costs.
             *
             * <p>Suppose <tt>x</tt> is a set known to contain only strings.
             * The following code can be used to dump the set into a newly allocated
             * array of <tt>String</tt>:
             *
             * <pre>
             *     String[] y = x.toArray(new String[0]);</pre>
             * <p>
             * Note that <tt>toArray(new Object[0])</tt> is identical in function to
             * <tt>toArray()</tt>.
             *
             * @param a the array into which the elements of this set are to be
             *          stored, if it is big enough; otherwise, a new array of the same
             *          runtime type is allocated for this purpose.
             * @return an array containing all the elements in this set
             * @throws ArrayStoreException  if the runtime type of the specified array
             *                              is not a supertype of the runtime type of every element in this
             *                              set
             * @throws NullPointerException if the specified array is null
             */
            @Override
            public <T> T[] toArray(T[] a) {
                return Lambda.asList(iterator()).toArray(a);
            }

            /**
             * Adds the specified element to this set if it is not already present
             * (optional operation).  More formally, adds the specified element
             * <tt>e</tt> to this set if the set contains no element <tt>e2</tt>
             * such that
             * <tt>(e==null&nbsp;?&nbsp;e2==null&nbsp;:&nbsp;e.equals(e2))</tt>.
             * If this set already contains the element, the call leaves the set
             * unchanged and returns <tt>false</tt>.  In combination with the
             * restriction on constructors, this ensures that sets never contain
             * duplicate elements.
             *
             * <p>The stipulation above does not imply that sets must accept all
             * elements; sets may refuse to add any particular element, including
             * <tt>null</tt>, and throw an exception, as described in the
             * specification for {@link Collection#add Collection.add}.
             * Individual set implementations should clearly document any
             * restrictions on the elements that they may contain.
             *
             * @param s element to be added to this set
             * @return <tt>true</tt> if this set did not already contain the specified
             * element
             * @throws UnsupportedOperationException if the <tt>add</tt> operation
             *                                       is not supported by this set
             * @throws ClassCastException            if the class of the specified element
             *                                       prevents it from being added to this set
             * @throws NullPointerException          if the specified element is null and this
             *                                       set does not permit null elements
             * @throws IllegalArgumentException      if some property of the specified element
             *                                       prevents it from being added to this set
             */
            @Override
            public boolean add(String s) {
                throw new UnsupportedOperationException();
            }

            /**
             * Removes the specified element from this set if it is present
             * (optional operation).  More formally, removes an element <tt>e</tt>
             * such that
             * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>, if
             * this set contains such an element.  Returns <tt>true</tt> if this set
             * contained the element (or equivalently, if this set changed as a
             * result of the call).  (This set will not contain the element once the
             * call returns.)
             *
             * @param o object to be removed from this set, if present
             * @return <tt>true</tt> if this set contained the specified element
             * @throws ClassCastException            if the type of the specified element
             *                                       is incompatible with this set
             *                                       (<a href="Collection.html#optional-restrictions">optional</a>)
             * @throws NullPointerException          if the specified element is null and this
             *                                       set does not permit null elements
             *                                       (<a href="Collection.html#optional-restrictions">optional</a>)
             * @throws UnsupportedOperationException if the <tt>remove</tt> operation
             *                                       is not supported by this set
             */
            @Override
            public boolean remove(Object o) {
                throw new UnsupportedOperationException();
            }

            /**
             * Returns <tt>true</tt> if this set contains all of the elements of the
             * specified collection.  If the specified collection is also a set, this
             * method returns <tt>true</tt> if it is a <i>subset</i> of this set.
             *
             * @param c collection to be checked for containment in this set
             * @return <tt>true</tt> if this set contains all of the elements of the
             * specified collection
             * @throws ClassCastException   if the types of one or more elements
             *                              in the specified collection are incompatible with this
             *                              set
             *                              (<a href="Collection.html#optional-restrictions">optional</a>)
             * @throws NullPointerException if the specified collection contains one
             *                              or more null elements and this set does not permit null
             *                              elements
             *                              (<a href="Collection.html#optional-restrictions">optional</a>),
             *                              or if the specified collection is null
             * @see #contains(Object)
             */
            @Override
            public boolean containsAll(Collection<?> c) {
                for (Object obj : c) {
                    if (!MapVariable.this.containsKey(obj)) {
                        return false;
                    }
                }
                return true;
            }

            /**
             * Adds all of the elements in the specified collection to this set if
             * they're not already present (optional operation).  If the specified
             * collection is also a set, the <tt>addAll</tt> operation effectively
             * modifies this set so that its value is the <i>union</i> of the two
             * sets.  The behavior of this operation is undefined if the specified
             * collection is modified while the operation is in progress.
             *
             * @param c collection containing elements to be added to this set
             * @return <tt>true</tt> if this set changed as a result of the call
             * @throws UnsupportedOperationException if the <tt>addAll</tt> operation
             *                                       is not supported by this set
             * @throws ClassCastException            if the class of an element of the
             *                                       specified collection prevents it from being added to this set
             * @throws NullPointerException          if the specified collection contains one
             *                                       or more null elements and this set does not permit null
             *                                       elements, or if the specified collection is null
             * @throws IllegalArgumentException      if some property of an element of the
             *                                       specified collection prevents it from being added to this set
             * @see #add(Object)
             */
            @Override
            public boolean addAll(Collection<? extends String> c) {
                throw new UnsupportedOperationException();
            }

            /**
             * Retains only the elements in this set that are contained in the
             * specified collection (optional operation).  In other words, removes
             * from this set all of its elements that are not contained in the
             * specified collection.  If the specified collection is also a set, this
             * operation effectively modifies this set so that its value is the
             * <i>intersection</i> of the two sets.
             *
             * @param c collection containing elements to be retained in this set
             * @return <tt>true</tt> if this set changed as a result of the call
             * @throws UnsupportedOperationException if the <tt>retainAll</tt> operation
             *                                       is not supported by this set
             * @throws ClassCastException            if the class of an element of this set
             *                                       is incompatible with the specified collection
             *                                       (<a href="Collection.html#optional-restrictions">optional</a>)
             * @throws NullPointerException          if this set contains a null element and the
             *                                       specified collection does not permit null elements
             *                                       (<a href="Collection.html#optional-restrictions">optional</a>),
             *                                       or if the specified collection is null
             * @see #remove(Object)
             */
            @Override
            public boolean retainAll(Collection<?> c) {
                throw new UnsupportedOperationException();
            }

            /**
             * Removes from this set all of its elements that are contained in the
             * specified collection (optional operation).  If the specified
             * collection is also a set, this operation effectively modifies this
             * set so that its value is the <i>asymmetric set difference</i> of
             * the two sets.
             *
             * @param c collection containing elements to be removed from this set
             * @return <tt>true</tt> if this set changed as a result of the call
             * @throws UnsupportedOperationException if the <tt>removeAll</tt> operation
             *                                       is not supported by this set
             * @throws ClassCastException            if the class of an element of this set
             *                                       is incompatible with the specified collection
             *                                       (<a href="Collection.html#optional-restrictions">optional</a>)
             * @throws NullPointerException          if this set contains a null element and the
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
             * Removes all of the elements from this set (optional operation).
             * The set will be empty after this call returns.
             *
             * @throws UnsupportedOperationException if the <tt>clear</tt> method
             *                                       is not supported by this set
             */
            @Override
            public void clear() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     * The collection is backed by the map, so changes to the map are
     * reflected in the collection, and vice-versa.  If the map is
     * modified while an iteration over the collection is in progress
     * (except through the iterator's own <tt>remove</tt> operation),
     * the results of the iteration are undefined.  The collection
     * supports element removal, which removes the corresponding
     * mapping from the map, via the <tt>Iterator.remove</tt>,
     * <tt>Collection.remove</tt>, <tt>removeAll</tt>,
     * <tt>retainAll</tt> and <tt>clear</tt> operations.  It does not
     * support the <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a collection view of the values contained in this map
     */
    @Override
    public Collection<T> values() {
        return new AbstractCollection<T>() {
            @Override
            public Iterator<T> iterator() {
                return Lambda.map(MapVariable.this.variables(), entry -> entry.getValue().resolve(context()));
            }

            @Override
            public int size() {
                return MapVariable.this.size();
            }
        };
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own <tt>remove</tt> operation, or through the
     * <tt>setValue</tt> operation on a map entry returned by the
     * iterator) the results of the iteration are undefined.  The set
     * supports element removal, which removes the corresponding
     * mapping from the map, via the <tt>Iterator.remove</tt>,
     * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt> and
     * <tt>clear</tt> operations.  It does not support the
     * <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a set view of the mappings contained in this map
     */
    @Override
    public Set<Entry<String, T>> entrySet() {
        return new Set<Entry<String, T>>() {
            /**
             * Returns the number of elements in this set (its cardinality).  If this
             * set contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
             * <tt>Integer.MAX_VALUE</tt>.
             *
             * @return the number of elements in this set (its cardinality)
             */
            @Override
            public int size() {
                return MapVariable.this.size();
            }

            /**
             * Returns <tt>true</tt> if this set contains no elements.
             *
             * @return <tt>true</tt> if this set contains no elements
             */
            @Override
            public boolean isEmpty() {
                return MapVariable.this.isEmpty();
            }

            /**
             * Returns <tt>true</tt> if this set contains the specified element.
             * More formally, returns <tt>true</tt> if and only if this set
             * contains an element <tt>e</tt> such that
             * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
             *
             * @param o element whose presence in this set is to be tested
             * @return <tt>true</tt> if this set contains the specified element
             * @throws ClassCastException   if the type of the specified element
             *                              is incompatible with this set
             *                              (<a href="Collection.html#optional-restrictions">optional</a>)
             * @throws NullPointerException if the specified element is null and this
             *                              set does not permit null elements
             *                              (<a href="Collection.html#optional-restrictions">optional</a>)
             */
            @Override
            public boolean contains(Object o) {
                return o instanceof Entry && MapVariable.this.containsKey(((Entry) o).getKey());
            }

            /**
             * Returns an iterator over the elements in this set.  The elements are
             * returned in no particular order (unless this set is an instance of some
             * class that provides a guarantee).
             *
             * @return an iterator over the elements in this set
             */
            @Override
            public Iterator<Entry<String, T>> iterator() {
                return Lambda.map(MapVariable.this.variables(), v -> new Entry<String, T>() {
                    /**
                     * Returns the key corresponding to this entry.
                     *
                     * @return the key corresponding to this entry
                     * @throws IllegalStateException implementations may, but are not
                     *                               required to, throw this exception if the entry has been
                     *                               removed from the backing map.
                     */
                    @Override
                    public String getKey() {
                        return v.getKey();
                    }

                    /**
                     * Returns the value corresponding to this entry.  If the mapping
                     * has been removed from the backing map (by the iterator's
                     * <tt>remove</tt> operation), the results of this call are undefined.
                     *
                     * @return the value corresponding to this entry
                     * @throws IllegalStateException implementations may, but are not
                     *                               required to, throw this exception if the entry has been
                     *                               removed from the backing map.
                     */
                    @Override
                    public T getValue() {
                        return v.getValue().resolve(context());
                    }

                    /**
                     * Replaces the value corresponding to this entry with the specified
                     * value (optional operation).  (Writes through to the map.)  The
                     * behavior of this call is undefined if the mapping has already been
                     * removed from the map (by the iterator's <tt>remove</tt> operation).
                     *
                     * @param value new value to be stored in this entry
                     * @return old value corresponding to the entry
                     * @throws UnsupportedOperationException if the <tt>put</tt> operation
                     *                                       is not supported by the backing map
                     * @throws ClassCastException            if the class of the specified value
                     *                                       prevents it from being stored in the backing map
                     * @throws NullPointerException          if the backing map does not permit
                     *                                       null values, and the specified value is null
                     * @throws IllegalArgumentException      if some property of this value
                     *                                       prevents it from being stored in the backing map
                     * @throws IllegalStateException         implementations may, but are not
                     *                                       required to, throw this exception if the entry has been
                     *                                       removed from the backing map.
                     */
                    @Override
                    public T setValue(T value) {
                        throw new UnsupportedOperationException();
                    }
                });
            }

            /**
             * Returns an array containing all of the elements in this set.
             * If this set makes any guarantees as to what order its elements
             * are returned by its iterator, this method must return the
             * elements in the same order.
             *
             * <p>The returned array will be "safe" in that no references to it
             * are maintained by this set.  (In other words, this method must
             * allocate a new array even if this set is backed by an array).
             * The caller is thus free to modify the returned array.
             *
             * <p>This method acts as bridge between array-based and collection-based
             * APIs.
             *
             * @return an array containing all the elements in this set
             */
            @Override
            public Object[] toArray() {
                return Lambda.asList(iterator()).toArray();
            }

            /**
             * Returns an array containing all of the elements in this set; the
             * runtime type of the returned array is that of the specified array.
             * If the set fits in the specified array, it is returned therein.
             * Otherwise, a new array is allocated with the runtime type of the
             * specified array and the size of this set.
             *
             * <p>If this set fits in the specified array with room to spare
             * (i.e., the array has more elements than this set), the element in
             * the array immediately following the end of the set is set to
             * <tt>null</tt>.  (This is useful in determining the length of this
             * set <i>only</i> if the caller knows that this set does not contain
             * any null elements.)
             *
             * <p>If this set makes any guarantees as to what order its elements
             * are returned by its iterator, this method must return the elements
             * in the same order.
             *
             * <p>Like the {@link #toArray()} method, this method acts as bridge between
             * array-based and collection-based APIs.  Further, this method allows
             * precise control over the runtime type of the output array, and may,
             * under certain circumstances, be used to save allocation costs.
             *
             * <p>Suppose <tt>x</tt> is a set known to contain only strings.
             * The following code can be used to dump the set into a newly allocated
             * array of <tt>String</tt>:
             *
             * <pre>
             *     String[] y = x.toArray(new String[0]);</pre>
             * <p>
             * Note that <tt>toArray(new Object[0])</tt> is identical in function to
             * <tt>toArray()</tt>.
             *
             * @param a the array into which the elements of this set are to be
             *          stored, if it is big enough; otherwise, a new array of the same
             *          runtime type is allocated for this purpose.
             * @return an array containing all the elements in this set
             * @throws ArrayStoreException  if the runtime type of the specified array
             *                              is not a supertype of the runtime type of every element in this
             *                              set
             * @throws NullPointerException if the specified array is null
             */
            @Override
            public <T> T[] toArray(T[] a) {
                return Lambda.asList(iterator()).toArray(a);
            }

            /**
             * Adds the specified element to this set if it is not already present
             * (optional operation).  More formally, adds the specified element
             * <tt>e</tt> to this set if the set contains no element <tt>e2</tt>
             * such that
             * <tt>(e==null&nbsp;?&nbsp;e2==null&nbsp;:&nbsp;e.equals(e2))</tt>.
             * If this set already contains the element, the call leaves the set
             * unchanged and returns <tt>false</tt>.  In combination with the
             * restriction on constructors, this ensures that sets never contain
             * duplicate elements.
             *
             * <p>The stipulation above does not imply that sets must accept all
             * elements; sets may refuse to add any particular element, including
             * <tt>null</tt>, and throw an exception, as described in the
             * specification for {@link Collection#add Collection.add}.
             * Individual set implementations should clearly document any
             * restrictions on the elements that they may contain.
             *
             * @param stringObjectEntry element to be added to this set
             * @return <tt>true</tt> if this set did not already contain the specified
             * element
             * @throws UnsupportedOperationException if the <tt>add</tt> operation
             *                                       is not supported by this set
             * @throws ClassCastException            if the class of the specified element
             *                                       prevents it from being added to this set
             * @throws NullPointerException          if the specified element is null and this
             *                                       set does not permit null elements
             * @throws IllegalArgumentException      if some property of the specified element
             *                                       prevents it from being added to this set
             */
            @Override
            public boolean add(Entry<String, T> stringObjectEntry) {
                MapVariable.this.put(stringObjectEntry.getKey(), stringObjectEntry.getValue());
                return true;
            }

            /**
             * Removes the specified element from this set if it is present
             * (optional operation).  More formally, removes an element <tt>e</tt>
             * such that
             * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>, if
             * this set contains such an element.  Returns <tt>true</tt> if this set
             * contained the element (or equivalently, if this set changed as a
             * result of the call).  (This set will not contain the element once the
             * call returns.)
             *
             * @param o object to be removed from this set, if present
             * @return <tt>true</tt> if this set contained the specified element
             * @throws ClassCastException            if the type of the specified element
             *                                       is incompatible with this set
             *                                       (<a href="Collection.html#optional-restrictions">optional</a>)
             * @throws NullPointerException          if the specified element is null and this
             *                                       set does not permit null elements
             *                                       (<a href="Collection.html#optional-restrictions">optional</a>)
             * @throws UnsupportedOperationException if the <tt>remove</tt> operation
             *                                       is not supported by this set
             */
            @Override
            public boolean remove(Object o) {
                throw new UnsupportedOperationException();
            }

            /**
             * Returns <tt>true</tt> if this set contains all of the elements of the
             * specified collection.  If the specified collection is also a set, this
             * method returns <tt>true</tt> if it is a <i>subset</i> of this set.
             *
             * @param c collection to be checked for containment in this set
             * @return <tt>true</tt> if this set contains all of the elements of the
             * specified collection
             * @throws ClassCastException   if the types of one or more elements
             *                              in the specified collection are incompatible with this
             *                              set
             *                              (<a href="Collection.html#optional-restrictions">optional</a>)
             * @throws NullPointerException if the specified collection contains one
             *                              or more null elements and this set does not permit null
             *                              elements
             *                              (<a href="Collection.html#optional-restrictions">optional</a>),
             *                              or if the specified collection is null
             * @see #contains(Object)
             */
            @Override
            public boolean containsAll(Collection<?> c) {
                for (Object o : c) {
                    if (!contains(o)) {
                        return false;
                    }
                }
                return true;
            }

            /**
             * Adds all of the elements in the specified collection to this set if
             * they're not already present (optional operation).  If the specified
             * collection is also a set, the <tt>addAll</tt> operation effectively
             * modifies this set so that its value is the <i>union</i> of the two
             * sets.  The behavior of this operation is undefined if the specified
             * collection is modified while the operation is in progress.
             *
             * @param c collection containing elements to be added to this set
             * @return <tt>true</tt> if this set changed as a result of the call
             * @throws UnsupportedOperationException if the <tt>addAll</tt> operation
             *                                       is not supported by this set
             * @throws ClassCastException            if the class of an element of the
             *                                       specified collection prevents it from being added to this set
             * @throws NullPointerException          if the specified collection contains one
             *                                       or more null elements and this set does not permit null
             *                                       elements, or if the specified collection is null
             * @throws IllegalArgumentException      if some property of an element of the
             *                                       specified collection prevents it from being added to this set
             * @see #add(Object)
             */
            @Override
            public boolean addAll(Collection<? extends Entry<String, T>> c) {
                for (Entry<String, T> entry : c) {
                    add(entry);
                }
                return true;
            }

            /**
             * Retains only the elements in this set that are contained in the
             * specified collection (optional operation).  In other words, removes
             * from this set all of its elements that are not contained in the
             * specified collection.  If the specified collection is also a set, this
             * operation effectively modifies this set so that its value is the
             * <i>intersection</i> of the two sets.
             *
             * @param c collection containing elements to be retained in this set
             * @return <tt>true</tt> if this set changed as a result of the call
             * @throws UnsupportedOperationException if the <tt>retainAll</tt> operation
             *                                       is not supported by this set
             * @throws ClassCastException            if the class of an element of this set
             *                                       is incompatible with the specified collection
             *                                       (<a href="Collection.html#optional-restrictions">optional</a>)
             * @throws NullPointerException          if this set contains a null element and the
             *                                       specified collection does not permit null elements
             *                                       (<a href="Collection.html#optional-restrictions">optional</a>),
             *                                       or if the specified collection is null
             * @see #remove(Object)
             */
            @Override
            public boolean retainAll(Collection<?> c) {
                throw new UnsupportedOperationException();
            }

            /**
             * Removes from this set all of its elements that are contained in the
             * specified collection (optional operation).  If the specified
             * collection is also a set, this operation effectively modifies this
             * set so that its value is the <i>asymmetric set difference</i> of
             * the two sets.
             *
             * @param c collection containing elements to be removed from this set
             * @return <tt>true</tt> if this set changed as a result of the call
             * @throws UnsupportedOperationException if the <tt>removeAll</tt> operation
             *                                       is not supported by this set
             * @throws ClassCastException            if the class of an element of this set
             *                                       is incompatible with the specified collection
             *                                       (<a href="Collection.html#optional-restrictions">optional</a>)
             * @throws NullPointerException          if this set contains a null element and the
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
             * Removes all of the elements from this set (optional operation).
             * The set will be empty after this call returns.
             *
             * @throws UnsupportedOperationException if the <tt>clear</tt> method
             *                                       is not supported by this set
             */
            @Override
            public void clear() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
