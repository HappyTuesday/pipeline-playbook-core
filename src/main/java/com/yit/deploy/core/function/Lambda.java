package com.yit.deploy.core.function;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

public class Lambda {
    public static <T> List<T> newList(int size) {
        return newList(size, null);
    }

    public static <T> List<T> newList(int size, T defaultValue) {
        List<T> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(i, defaultValue);
        }
        return list;
    }

    /**
     * iterate each element of the list, and return the first element on which the predicate passed
     * @param list
     * @param predicate
     * @param <T>
     * @return
     */
    public static <T> boolean each(Collection<T> list, Predicate<T> predicate) {
        for (T x : list) {
            if (predicate.test(x)) return true;
        }
        return false;
    }

    /**
     * iterate each element of the list in reverse order, and return the first element on which the predicate passed
     * @param list
     * @param predicate
     * @param <T>
     * @return
     */
    public static <T> boolean eachReverse(List<T> list, Predicate<T> predicate) {
        for (int i = 0; i < list.size(); i++) {
            if (predicate.test(list.get(i))) return true;
        }
        return false;
    }

    public static <K, V> boolean each(Map<K, V> map, BiPredicate<K, V> predicate) {
        for (K key : map.keySet()) {
            if (predicate.test(key, map.get(key))) return true;
        }
        return false;
    }

    public static <T> T find(T[] list, Predicate<T> predicate) {
        for (T x : list) {
            if (predicate.test(x)) return x;
        }
        return null;
    }

    public static <T> T find(Collection<T> list, Predicate<T> predicate) {
        for (T x : list) {
            if (predicate.test(x)) return x;
        }
        return null;
    }

    public static <T> int findIndexOf(Collection<T> list, Predicate<T> predicate) {
        int i = 0;
        for (T x : list) {
            if (predicate.test(x)) return i;
            i++;
        }
        return -1;
    }

    public static <T> List<T> findAll(Collection<T> list, Predicate<T> predicate) {
        List<T> target = new ArrayList<>(list.size());
        for (T x : list) {
            if (predicate.test(x)) target.add(x);
        }
        return target;
    }

    public static <T, R> List<R> findAll(Collection<T> list, Predicate<T> predicate, Function<T, R> map) {
        List<R> target = new ArrayList<>(list.size());
        for (T x : list) {
            if (predicate.test(x)) target.add(map.apply(x));
        }
        return target;
    }

    public static <T> Iterable<T> filter(Iterable<T> iterable, Predicate<T> predicate) {
        return () -> filter(iterable.iterator(), (t, i) -> predicate.test(t));
    }

    public static <T> Iterator<T> filter(Iterator<T> iter, Predicate<T> predicate) {
        return filter(iter, (t, i) -> predicate.test(t));
    }

    public static <T> Iterator<T> filter(Iterator<T> iter, AnyInt2Any<T, Boolean> predicate) {
        return new Iterator<T>() {

            int i = 0;
            boolean hasNext;
            T next;

            @Override
            public boolean hasNext() {
                if (hasNext) {
                    return true;
                }

                while (iter.hasNext()) {
                    if (predicate.apply(next = iter.next(), i++)) {
                        return hasNext = true;
                    }
                }
                return hasNext = false;
            }

            @Override
            public T next() {
                if (!hasNext) {
                    throw new NoSuchElementException();
                }
                hasNext = false;
                return next;
            }
        };
    }

    public static <T, R> Iterator<R> map(Iterator<T> iter, Function<T, R> mapper) {
        return new Iterator<R>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public R next() {
                return mapper.apply(iter.next());
            }
        };
    }

    public static <T, R> Iterator<R> map(Iterator<T> iter, AnyInt2Any<T, R> mapper) {
        return new Iterator<R>() {
            int i = 0;
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public R next() {
                return mapper.apply(iter.next(), i++);
            }
        };
    }

    public static <T, R> ListIterator<R> map(ListIterator<T> iter, Function<T, R> mapper) {
        return new ListIterator<R>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public R next() {
                return mapper.apply(iter.next());
            }

            @Override
            public boolean hasPrevious() {
                return iter.hasPrevious();
            }

            @Override
            public R previous() {
                return mapper.apply(iter.previous());
            }

            @Override
            public int nextIndex() {
                return iter.nextIndex();
            }

            @Override
            public int previousIndex() {
                return iter.previousIndex();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void set(R r) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void add(R r) {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static <T> long count(Collection<T> source, Predicate<T> filter) {
        return reduce(source, (x, c) -> filter.test(x) ? c + 1 : c);
    }

    public static <T, TR> TR reduce(Collection<T> list, TR starter, BiFunction<T, TR, TR> reducer) {
        for (T x : list) {
            starter = reducer.apply(x, starter);
        }
        return starter;
    }

    public static <T> long reduce(Collection<T> list, AnyLongToLongFunction<T> reducer) {
        return reduce(list, 0, reducer);
    }

    public static <T> long reduce(Collection<T> list, long starter, AnyLongToLongFunction<T> reducer) {
        for (T x : list) {
            starter = reducer.apply(x, starter);
        }
        return starter;
    }

    public static <T, R> List<R> map(T[] list, Function<T, R> f) {
        List<R> result = new ArrayList<>(list.length);
        for (T x : list) {
            result.add(f.apply(x));
        }
        return result;
    }

    public static <T, R> List<R> map(Collection<T> list, Function<T, R> f) {
        List<R> result = new ArrayList<>(list.size());
        for (T x : list) {
            result.add(f.apply(x));
        }
        return result;
    }

    public static <K, V, R> List<R> map(Map<K, V> map, BiFunction<K, V, R> f) {
        List<R> result = new ArrayList<>(map.size());
        for (K key : map.keySet()) {
            result.add(f.apply(key, map.get(key)));
        }
        return result;
    }

    public static <K, V, KR, VR> Map<KR, VR> map(Map<K, V> map, BiFunction<K, V, KR> keyExtractor, BiFunction<K, V, VR> valueExtractor) {
        Map<KR, VR> result = new HashMap<>(map.size());
        for (K key : map.keySet()) {
            V value = map.get(key);
            result.put(keyExtractor.apply(key, value), valueExtractor.apply(key, value));
        }
        return result;
    }

    public static <K, V, VR> Map<K, VR> mapValues(Map<K, V> map, Function<V, VR> valueExtractor) {
        Map<K, VR> result = new HashMap<>(map.size());
        for (K key : map.keySet()) {
            V value = map.get(key);
            result.put(key, valueExtractor.apply(value));
        }
        return result;
    }

    /**
     * create a new list without duplicate items from list
     * @param list
     * @param <T>
     * @return
     */
    public static <T> List<T> unique(Collection<T> list) {
        List<T> result = new ArrayList<>(list.size());
        OUTER:
        for (T x : list) {
            for (T y : result) {
                if (Objects.equals(x, y)) continue OUTER;
            }
            result.add(x);
        }
        return result;
    }

    public static <T> List<T> concatOne(Collection<T> a, T x) {
        List<T> list = new ArrayList<>(a.size() + 1);
        list.add(x);
        return list;
    }

    public static <T> List<T> concat(Collection<T> a, Collection<T> b) {
        List<T> list = new ArrayList<>(a.size() + b.size());
        list.addAll(a);
        list.addAll(b);
        return list;
    }

    public static <T> List<T> concat(T head, List<T> tail) {
        List<T> list = new ArrayList<>(1 + tail.size());
        list.add(head);
        list.addAll(tail);
        return list;
    }

    @SafeVarargs
    public static <T> List<T> concat(T head, T ... tail) {
        return concat(head, Arrays.asList(tail));
    }

    /**
     * return a if a is not null, otherwise return b
     */
    public static <T> T cascade(T a, T b) {
        return a == null ? b : a;
    }

    /**
     * return a if a is not null, otherwise return b
     */
    public static <T> T cascade(T a, T b, T c) {
        return a == null ? b == null ? c : b : a;
    }

    public static <T, R> R safeNavigate(T a, Function<T, R> navigator) {
        return a == null ? null : navigator.apply(a);
    }

    public static List<String> tokenize(String source ,String dem) {
        return asList(new StringTokenizer(source, dem), String.class);
    }

    public static List<String> tokenize(String source) {
        return asList(new StringTokenizer(source), String.class);
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> asList(Enumeration e, Class<T> clazz) {
        List<T> list = new ArrayList<>();
        while (e.hasMoreElements()) list.add((T) e.nextElement());
        return list;
    }

    @SafeVarargs
    public static <T> List<T> asList(T ... args) {
        return new ArrayList<>(Arrays.asList(args));
    }

    public static <K, V> Map<K, V> merge(Map<K, V> a, Map<K, V> b) {
        Map<K, V> c = new HashMap<>(a);
        c.putAll(b);
        return c;
    }

    public static <K, V> Map<K, V> put(Map<K, V> map, K key1, V value1) {
        map.put(key1, value1);
        return map;
    }

    public static boolean toBoolean(Object v) {
        if (v == null) {
            return false;
        } else if (v instanceof Boolean) {
            return (boolean) v;
        } else if (v instanceof Number) {
            return (int) v != 0;
        } else if (v instanceof Character) {
            return (char) v != 0;
        } else {
            return true;
        }
    }

    public static <T> String toString(Collection<T> c) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        boolean first = true;
        for (T t : c) {
            if (first) {
                first = false;
            } else {
                sb.append(',').append(' ');
            }
            sb.append(t);
        }
        sb.append(']');
        return sb.toString();
    }

    public static <K, V> String toString(Map<K, V> m) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        boolean first = true;
        for (Map.Entry<K, V> entry : m.entrySet()) {
            if (first) {
                first = false;
            } else {
                sb.append(',').append(' ');
            }
            sb.append(entry.getKey());
            sb.append(':').append(' ');
            sb.append(entry.getValue());
        }
        sb.append(']');
        return sb.toString();
    }

    public static <T> List<T> intersect(Collection<T> c1, Collection<T> c2) {
        List<T> list = new ArrayList<>(Math.min(c1.size(), c2.size()));
        for (T x1 : c1) {
            for (T x2 : c2) {
                if (Objects.equals(x1, x2)) {
                    list.add(x1);
                    break;
                }
            }
        }
        return list;
    }

    public static <T> List<T> except(Collection<T> c1, Collection<T> c2) {
        List<T> list = new ArrayList<>(c1.size());
        OUTER:
        for (T x1 : c1) {
            for (T x2 : c2) {
                if (Objects.equals(x1, x2)) continue OUTER;
            }
            list.add(x1);
        }
        return list;
    }

    public static <T> void uniqueAdd(Collection<T> c, T e) {
        if (!c.contains(e)) c.add(e);
    }

    public static <T> void uniqueAdd(Collection<T> c, Collection<T> es) {
        for (T e : es) {
            if (!c.contains(e)) c.add(e);
        }
    }

    public static <T> void uniqueAdd(Collection<T> c, T[] es) {
        for (T e : es) {
            if (!c.contains(e)) c.add(e);
        }
    }

    public static <T> int findIndexOf(T[] c, T e) {
        for (int i = 0; i < c.length; i++) {
            if (Objects.equals(c[i], e)) return i;
        }
        return -1;
    }

    public static <T> boolean contains(T[] c, T e) {
        return findIndexOf(c, e) >= 0;
    }

    public static <T> T last(List<T> list) {
        return list.get(list.size() - 1);
    }

    public static <T> T last(T[] list) {
        return list[list.length - 1];
    }

    public static <K, V> Map<K, V> toMap(List<K> list, Function<K, V> valueGenerator) {
        Map<K, V> map = new HashMap<>(list.size());
        for (K e : list) {
            map.put(e, valueGenerator.apply(e));
        }
        return map;
    }

    public static <E, K, V> Map<K, V> toMap(List<E> list, Function<E, K> keyGenerator, Function<E, V> valueGenerator) {
        Map<K, V> map = new HashMap<>(list.size());
        for (E e : list) {
            map.put(keyGenerator.apply(e), valueGenerator.apply(e));
        }
        return map;
    }

    public static <E, K, V> Map<K, V> toMap(E[] list, Function<E, K> keyGenerator, Function<E, V> valueGenerator) {
        Map<K, V> map = new HashMap<>(list.length);
        for (E e : list) {
            map.put(keyGenerator.apply(e), valueGenerator.apply(e));
        }
        return map;
    }

    public static Object subscript(Map map, String ... path) {
        return subscript(map, Object.class, true, path);
    }

    public static <T> T subscript(Map map, Class<T> clazz, String ... path) {
        return subscript(map, clazz, true, path);
    }

    @SuppressWarnings("unchecked")
    public static <T> T subscript(Map map, Class<T> clazz, boolean ignoreNull, String ... path) {
        Object value = map;
        for (String part : path) {
            if (value == null && ignoreNull) {
                return null;
            } else if (value instanceof Map) {
                value = ((Map)value).get(part);
            } else {
                throw new IllegalArgumentException("invalid path segment " + part);
            }
        }
        return (T) value;
    }

    public static <K, V> Map<K, V> asMap(K key1, V value1) {
        Map<K, V> map = new HashMap<>();
        map.put(key1, value1);
        return map;
    }

    public static <K, V> Map<K, V> asMap(K key1, V value1, K key2, V value2) {
        Map<K, V> map = asMap(key1, value1);
        map.put(key2, value2);
        return map;
    }

    public static <K, V> Map<K, V> asMap(K key1, V value1, K key2, V value2, K key3, V value3) {
        Map<K, V> map = asMap(key1, value1, key2, value2);
        map.put(key3, value3);
        return map;
    }

    public static <K, V> Map<K, V> asMap(K key1, V value1, K key2, V value2, K key3, V value3, K key4, V value4) {
        Map<K, V> map = asMap(key1, value1, key2, value2, key3, value3);
        map.put(key4, value4);
        return map;
    }

    public static String join(String dem, Collection<?> c) {
        List<String> list = new ArrayList<>(c.size());
        for (Object x : c) {
            list.add(String.valueOf(x));
        }
        return String.join(dem, list);
    }

    public static <K, V> Map<V, Set<K>> reverseMap(Map<K, V> source) {
        Map<V, Set<K>> map = new HashMap<>(source.size());
        for (K key : source.keySet()) {
            V value = source.get(key);
            map.computeIfAbsent(value, x -> new HashSet<>()).add(key);
        }
        return map;
    }

    public static <T> List<T> sort(Collection<T> c) {
        List<T> list = new ArrayList<>(c);
        list.sort(null);
        return list;
    }

    public static <T> boolean all(T[] c, Predicate<T> test) {
        for (T x : c) {
            if (!test.test(x)) return false;
        }
        return true;
    }

    public static <T> boolean all(Collection<T> c, Predicate<T> test) {
        for (T x : c) {
            if (!test.test(x)) return false;
        }
        return true;
    }

    public static <T> boolean any(T[] c, Predicate<T> test) {
        for (T x : c) {
            if (test.test(x)) return true;
        }
        return false;
    }

    public static <T> boolean any(Collection<T> c, Predicate<T> test) {
        for (T x : c) {
            if (test.test(x)) return true;
        }
        return false;
    }

    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static String empty2null(String s) {
        return s == null || s.isEmpty() ? null : s;
    }

    public static <T> boolean equalsIgnoreOrder(Collection<T> c1, Collection<T> c2) {
        if (c1.size() != c2.size()) return false;
        for (T x : c1) {
            boolean found = false;
            for (T y : c2) {
                if (Objects.equals(x, y)) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    public static Object clone(Object a) {
        try {
            if (a == null) return null;
            if (a instanceof Cloneable) {
                return a.getClass().getMethod("clone").invoke(a);
            }
            Class<?> c = a.getClass();
            Object r = c.newInstance();
            for (Field field : c.getDeclaredFields()) {
                field.set(r, field.get(a));
            }

            return r;
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Type getListElementType(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            Type rawType = pt.getRawType();
            if (rawType instanceof Class<?> && List.class.isAssignableFrom((Class<?>) rawType)) {
                Type[] ps = pt.getActualTypeArguments();
                if (ps != null && ps.length == 1) {
                    return ps[0];
                } else {
                    return Object.class;
                }
            }
        }

        throw new IllegalArgumentException("type " + type + " is not list");
    }

    public static Type[] getMapKVTypes(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            Type rawType = pt.getRawType();
            if (rawType instanceof Class<?> && Map.class.isAssignableFrom((Class<?>) rawType)) {
                Type[] ps = pt.getActualTypeArguments();
                if (ps != null && ps.length == 2) {
                    return ps;
                } else {
                    return new Type[]{Object.class, Object.class};
                }
            }
        }

        throw new IllegalArgumentException("type " + type + " is not map");
    }

    public static List<Field> getPropertyFields(Class<?> clazz) {
        LinkedList<Field> fields = new LinkedList<>();
        for (Field field : clazz.getDeclaredFields()) {
            int mode = field.getModifiers();
            if (Modifier.isStatic(mode)) continue;
            field.setAccessible(true);
            fields.push(field);
        }

        return fields;
    }

    public static <T> void foreach(Iterable<T> c, Consumer<T> action) {
        for (T t : c) {
            action.accept(t);
        }
    }

    public static <T> void appendOrReplace(List<T> list, T t) {
        int i = list.indexOf(t);
        if (i < 0) {
            list.add(t);
        } else {
            list.set(i, t);
        }
    }

    public static <T> List<T> asList(Iterator<T> iterator) {
        List<T> list = new ArrayList<>();
        while (iterator.hasNext()) {
            T t = iterator.next();
            list.add(t);
        }
        return list;
    }

    public static <T> boolean contains(Iterator<T> iterator, Object o) {
        while (iterator.hasNext()) {
            if (Objects.equals(iterator.next(), o)) return true;
        }
        return false;
    }

    public static <T> List<T> newList(int size, Function<Integer, T> initializer) {
        List<T> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(initializer.apply(i));
        }
        return list;
    }

    public static <T> List<T> concatList(List<T> list1, List<T> list2) {
        if (list1.isEmpty()) {
            return list2;
        } else if (list2.isEmpty()) {
            return list1;
        } else {
            List<T> ls = new ArrayList<>(list1.size() + list2.size());
            ls.addAll(list1);
            ls.addAll(list2);
            return ls;
        }
    }

    public static <T> List<T> concatList(List<T> list, T t) {
        if (list.isEmpty()) {
            return Collections.singletonList(t);
        } else {
            List<T> ls = new ArrayList<>(list.size() + 1);
            ls.addAll(list);
            ls.add(t);
            return ls;
        }
    }

    public static <K, V> Map<K, V> concatMap(Map<K, V> map1, Map<K, V> map2) {
        if (map1.isEmpty()) {
            return map2;
        } else if (map2.isEmpty()) {
            return map1;
        } else {
            Map<K, V> map = new HashMap<>(map1.size() + map2.size());
            map.putAll(map1);
            map.putAll(map2);
            return map;
        }
    }

    public static <K, V> Map<K, V> concatMap(Map<K, V> map1, K key, V value) {
        if (map1.isEmpty()) {
            return Collections.singletonMap(key, value);
        } else {
            Map<K, V> map = new HashMap<>(map1.size() + 1);
            map.putAll(map1);
            map.put(key, value);
            return map;
        }
    }

    public static Iterator<Object> EMPTY_ITERATOR = new Iterator<Object>() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException();
        }
    };

    @SuppressWarnings("unchecked")
    public static <T> Iterator<T> emptyIterator() {
        return (Iterator<T>) EMPTY_ITERATOR;
    }

    public static int size(Iterator<?> iter) {
        int c = 0;
        while(iter.hasNext()) {
            c++;
            iter.next();
        }
        return c;
    }

    public static <T> T getAt(Iterator<T> iter, int index, T defaults) {
        while (iter.hasNext()) {
            if (index-- <= 0) {
                return iter.next();
            } else {
                iter.next();
            }
        }
        return defaults;
    }

    public static <T> Iterator<T> singleIterator(T t) {
        return new Iterator<T>() {

            boolean hasNext = true;

            @Override
            public boolean hasNext() {
                return hasNext;
            }

            @Override
            public T next() {
                if (!hasNext) {
                    throw new NoSuchElementException();
                }
                hasNext = false;
                return t;
            }
        };
    }

    public static <T, R> R reduce(Iterator<T> iter, R initial, BiFunction<T, R, R> f) {
        while (iter.hasNext()) {
            initial = f.apply(iter.next(), initial);
        }
        return initial;
    }

    public static <T> int reduce(Iterator<T> iter, int initial, AnyInt2Int<T> f) {
        while (iter.hasNext()) {
            initial = f.apply(iter.next(), initial);
        }
        return initial;
    }

    public static <T> boolean any(Iterator<T> iter, Predicate<T> predicate) {
        while (iter.hasNext()) {
            if (predicate.test(iter.next())) {
                return true;
            }
        }
        return false;
    }

    public static <T> Set<T> toSet(Iterator<T> iter) {
        Set<T> set = new HashSet<>();
        while (iter.hasNext()) {
            set.add(iter.next());
        }
        return set;
    }

    public static <T, R> Set<R> collectSet(Iterable<T> iter, Function<T, Collection<R>> f) {
        Set<R> set = new HashSet<>();
        for (T t : iter) {
            set.addAll(f.apply(t));
        }
        return set;
    }

    public static <T, K, V> Map<K, V> collectMap(Iterable<T> iter, Function<T, Map<K, V>> f) {
        Map<K, V> map = new HashMap<>();
        for (T t : iter) {
            map.putAll(f.apply(t));
        }
        return map;
    }

    public static <T, R> R cascade(Iterable<T> iter, Function<T, R> f) {
        for (T t : iter) {
            R r = f.apply(t);
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    public static <T> T[] append(T[] a, T s) {
        T[] b = Arrays.copyOf(a, a.length + 1);
        b[b.length - 1] = s;
        return b;
    }

    /**
     * create an iterator which iterates all values returned by the findNext util it supplied with a null
     * @param findNext the supplier used to get the next non-null value to iterate
     * @param <T> type
     * @return iterator
     */
    public static <T> Iterator<T> iterate(Supplier<T> findNext) {
        return new Iterator<T>() {
            T next;

            @Override
            public boolean hasNext() {
                if (next == null) {
                    next = findNext.get();
                }
                return next != null;
            }

            @Override
            public T next() {
                if (next == null) {
                    throw new NoSuchElementException();
                }
                T t = next;
                next = null;
                return t;
            }
        };
    }

    @SafeVarargs
    public static <T> Iterator<T> concat(Iterator<T>... iterators) {
        return new Iterator<T>() {

            int i = 0;
            boolean hasNext;
            T next;

            @Override
            public boolean hasNext() {
                if (hasNext) {
                    return true;
                }
                while (i < iterators.length) {
                    Iterator<T> iter = iterators[i++];
                    if (iter.hasNext()) {
                        hasNext = true;
                        next = iter.next();
                        return true;
                    }
                }
                return false;
            }

            @Override
            public T next() {
                if (!hasNext) {
                    throw new NoSuchElementException();
                }
                hasNext = false;
                return next;
            }
        };
    }

    @SafeVarargs
    public static <T> Iterable<T> concat(Iterable<T>... iterables) {
        @SuppressWarnings("unchecked")
        Iterator<T>[] iterators = new Iterator[iterables.length];
        for (int i = 0; i < iterables.length; i++) {
            iterators[i] = iterables[i].iterator();
        }
        return () -> concat(iterators);
    }

    public static <T, R> Iterable<R> map(Iterable<T> iterable, Function<T, R> f) {
        return () -> map(iterable.iterator(), f);
    }

    public static <K, V> boolean equals(Map<K, V> map1, Map<K, V> map2) {
        if (map1 == map2) {
            return true;
        }

        if (map1 == null || map2 == null) {
            return false;
        }

        for (K key : map1.keySet()) {
            if (map2.containsKey(key)) {
                if (!Objects.equals(map1.get(key), map2.get(key))) {
                    return false;
                }
            } else {
                return false;
            }
        }

        for (K key : map2.keySet()) {
            if (!map1.containsKey(key)) {
                return false;
            }
        }

        return true;
    }

    public static <T, R> List<R> collectList(Collection<T> c, Function<T, Collection<R>> f) {
        List<R> list = new ArrayList<>(c.size());
        for (T t : c) {
            list.addAll(f.apply(t));
        }
        return list;
    }

    /**
     * Iterates through the given Iterator, passing in the initial value to
     * the closure along with the first item. The result is passed back (injected) into
     * the closure along with the second item. The new result is injected back into
     * the closure along with the third item and so on until the Iterator has been
     * expired of values. Also known as foldLeft in functional parlance.
     *
     * @param self         an Iterator
     * @param initialValue some initial value
     * @param f      a closure
     * @return the result of the last closure call
     * @since 1.5.0
     */
    public static <E, T> T inject(Iterator<E> self, T initialValue, BiFunction<T, E, T> f) {
        T value = initialValue;
        while (self.hasNext()) {
            E item = self.next();
            value = f.apply(value, item);
        }
        return value;
    }
}
