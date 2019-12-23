package com.yit.deploy.core.collections;

import java.util.*;
import java.util.function.Function;

public class TransformMap<K, S, V> extends AbstractMap<K, V> {

    private final Map<K, S> map;
    private final Function<S, V> transformer;

    public TransformMap(Map<K, S> map, Function<S, V> transformer) {
        this.map = map;
        this.transformer = transformer;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation returns <tt>entrySet().size()</tt>.
     */
    @Override
    public int size() {
        return map.size();
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation returns <tt>size() == 0</tt>.
     */
    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * {@inheritDoc}
     *
     * @param key
     * @throws ClassCastException   {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @implSpec This implementation iterates over <tt>entrySet()</tt> searching
     * for an entry with the specified key.  If such an entry is found,
     * <tt>true</tt> is returned.  If the iteration terminates without
     * finding such an entry, <tt>false</tt> is returned.  Note that this
     * implementation requires linear time in the size of the map; many
     * implementations will override this method.
     */
    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    /**
     * {@inheritDoc}
     *
     * @param key
     * @throws ClassCastException   {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @implSpec This implementation iterates over <tt>entrySet()</tt> searching
     * for an entry with the specified key.  If such an entry is found,
     * the entry's value is returned.  If the iteration terminates without
     * finding such an entry, <tt>null</tt> is returned.  Note that this
     * implementation requires linear time in the size of the map; many
     * implementations will override this method.
     */
    @Override
    public V get(Object key) {
        return transformer.apply(map.get(key));
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return new AbstractSet<Entry<K, V>>() {
            /**
             * Returns an iterator over the elements contained in this collection.
             *
             * @return an iterator over the elements contained in this collection
             */
            @Override
            public Iterator<Entry<K, V>> iterator() {

                Iterator<Entry<K, S>> iter = map.entrySet().iterator();

                return new Iterator<Entry<K, V>>() {
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
                    public Entry<K, V> next() {
                        Entry<K, S> n = iter.next();
                        return new Entry<K, V>() {
                            /**
                             * Returns the key corresponding to this entry.
                             *
                             * @return the key corresponding to this entry
                             * @throws IllegalStateException implementations may, but are not
                             *                               required to, throw this exception if the entry has been
                             *                               removed from the backing map.
                             */
                            @Override
                            public K getKey() {
                                return n.getKey();
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
                            public V getValue() {
                                return transformer.apply(n.getValue());
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
                            public V setValue(V value) {
                                throw new UnsupportedOperationException();
                            }
                        };
                    }
                };
            }

            @Override
            public int size() {
                return map.size();
            }
        };
    }
}
