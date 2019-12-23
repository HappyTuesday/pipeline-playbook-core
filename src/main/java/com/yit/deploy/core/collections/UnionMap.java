package com.yit.deploy.core.collections;

import com.yit.deploy.core.function.Lambda;

import java.util.*;

public class UnionMap<K, V> extends AbstractMap<K, V> {
    private final Map<K, V> left, right;

    public UnionMap(Map<K, V> left, Map<K, V> right) {
        this.left = left;
        this.right = right;
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings
     */
    @Override
    public boolean isEmpty() {
        return left.isEmpty() && right.isEmpty();
    }

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified
     * key.  More formally, returns <tt>true</tt> if and only if
     * this map contains a mapping for a key <tt>k</tt> such that
     * <tt>(key==null ? k==null : key.equals(k))</tt>.  (There can be
     * at most one such mapping.)
     *
     * @param key key whose presence in this map is to be tested
     * @return <tt>true</tt> if this map contains a mapping for the specified
     * key
     * @throws ClassCastException   if the key is of an inappropriate type for
     *                              this map
     *                              (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified key is null and this map
     *                              does not permit null keys
     *                              (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     */
    @Override
    public boolean containsKey(Object key) {
        return left.containsKey(key) || right.containsKey(key);
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
    public V get(Object key) {
        V v = right.get(key);
        if (v != null) {
            return v;
        }
        return left.get(key);
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
    public Set<Entry<K, V>> entrySet() {
        return new AbstractSet<Entry<K, V>>() {
            @Override
            public Iterator<Entry<K, V>> iterator() {
                return new Iterator<Entry<K, V>>() {

                    Iterator<Entry<K, V>> iter = right.entrySet().iterator();
                    Entry<K, V> next;
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
                        if (next != null) {
                            return true;
                        }

                        while (true) {
                            if (iter.hasNext()) {
                                next = iter.next();
                                if (first || !right.containsKey(next.getKey())) {
                                    return true;
                                }
                                next = null;
                            } else if (first) {
                                iter = left.entrySet().iterator();
                                first = false;
                            } else {
                                return false;
                            }
                        }
                    }

                    /**
                     * Returns the next element in the iteration.
                     *
                     * @return the next element in the iteration
                     * @throws NoSuchElementException if the iteration has no more elements
                     */
                    @Override
                    public Entry<K, V> next() {
                        if (next == null) {
                            throw new NoSuchElementException();
                        }

                        Entry<K, V> entry = next;
                        next = null;
                        return entry;
                    }
                };
            }

            @Override
            public int size() {
                return Lambda.size(iterator());
            }
        };
    }
}
