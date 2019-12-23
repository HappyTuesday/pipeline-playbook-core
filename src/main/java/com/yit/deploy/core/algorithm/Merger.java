package com.yit.deploy.core.algorithm;

import com.yit.deploy.core.function.Lambda;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Merger<T> {
    /**
     * merge that instance into this instance
     * @param that another instance to be merged
     */
    void merge(T that);

    /**
     * merge a map into another
     * @param map the map merging into
     * @param more another map to be merged
     * @param <K> key type
     * @param <V> value type
     */
    static <K, M extends Merger<M>, V extends M> void merge(Map<K, V> map, Map<K, V> more, Function<K, V> creator) {
        for (K key : more.keySet()) {
            map.computeIfAbsent(key, creator).merge(more.get(key));
        }
    }

    /**
     * merge a key value pair into another
     * @param map the map merging into
     * @param key another key to be merged
     * @param more another value to be merged
     * @param <K> key type
     * @param <V> value type
     */
    static <K, M extends Merger<M>, V extends M> void merge(Map<K, V> map, K key, V more, Function<K, V> creator) {
        map.computeIfAbsent(key, creator).merge(more);
    }

    /**
     * merge a list into another
     * @param list the collection merging into
     * @param more another collection to be merge
     * @param <T> collection element type
     */
    static <M extends Merger<M>, T extends M, I> void merge(List<T> list, Iterable<T> more, Supplier<T> creator, Function<T, I> id) {
        for (T t : more) {
            I i = id.apply(t);
            int index = Lambda.findIndexOf(list, x -> Objects.equals(i, id.apply(x)));
            T o = index < 0 ? creator.get() : list.get(index);
            o.merge(t);
            if (index < 0) {
                list.add(o);
            } else {
                list.set(index, o);
            }
        }
    }
}
