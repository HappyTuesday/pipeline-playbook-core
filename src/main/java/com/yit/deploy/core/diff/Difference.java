package com.yit.deploy.core.diff;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.yit.deploy.core.function.Lambda;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Predicate;

public class Difference {

    private static final String SEP = "/";

    private final boolean ignoreEmptyString;
    private final Gson gson;
    private final Predicate<Field> fieldInclusionStrategy;

    public Difference() {
        this(false);
    }

    public Difference(boolean ignoreEmptyString) {
        this(ignoreEmptyString, null);
    }

    public Difference(boolean ignoreEmptyString, Predicate<Field> fieldInclusionStrategy) {
        this.ignoreEmptyString = ignoreEmptyString;
        gson = new GsonBuilder().addSerializationExclusionStrategy(new SerializationExclusionStrategy()).setPrettyPrinting().create();
        this.fieldInclusionStrategy = fieldInclusionStrategy;
    }

    private class SerializationExclusionStrategy implements ExclusionStrategy {

        /**
         * @param f the field object that is under test
         * @return true if the field should be ignored; otherwise false
         */
        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            return f.getAnnotation(ExcludeDiff.class) != null;
        }

        /**
         * @param clazz the class object that is under test
         * @return true if the class should be ignored; otherwise false
         */
        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }
    }

    private MatchResult innerJoin(List<?> a, List<?> b) {
        MatchResult m = new MatchResult();
        Map<Object, LinkedList<Integer>> map = new HashMap<>();
        for (int i = 0; i < a.size(); i++) {
            Object o = a.get(i);
            Object id = o instanceof Datum ? ((Datum) o).getId() : o;
            map.computeIfAbsent(id, key -> new LinkedList<>()).push(i);
        }
        for (int j = 0; j < b.size(); j++) {
            Object o = b.get(j);
            Object id = o instanceof Datum ? ((Datum) o).getId() : o;
            LinkedList<Integer> indices = map.get(id);
            if (indices != null && !indices.isEmpty()) {
                int i = indices.removeFirst();
                m.left.put(i, j);
                m.right.put(j, i);
            }
        }

        return m;
    }

    public static class MatchResult {
        public final Map<Integer, Integer> left = new HashMap<>();
        public final Map<Integer, Integer> right = new HashMap<>();
    }

    public List<Change> diff(Object a, Object b) {
        List<Change> changes = new ArrayList<>();
        Set<Object> close = new HashSet<>();
        diff(a, b, "", changes, close);
        return changes;
    }

    private Object preProcess(Object a) {
        if (ignoreEmptyString) {
            if ("".equals(a)) {
                return null;
            }
        }
        return a;
    }

    @SuppressWarnings("unchecked")
    private void diff(Object a, Object b, String path, List<Change> changes, Set<Object> close) {
        a = preProcess(a);
        b = preProcess(b);

        if (a == b || Objects.equals(a, b)) {
            return;
        }

        if (a == null ^ b == null) { // one is null
            changes.add(Change.changed(path, serializeObject(b)));
            return;
        }

        Class<?> ac = a.getClass(), bc = b.getClass();
        if (ac != bc) { // we do not get the detail diff of two objects with different types
            changes.add(Change.changed(path, serializeObject(b)));
            return;
        }

        if (ac.isArray()) {
            diff(Arrays.asList((Object[]) a), Arrays.asList((Object[]) a), path, changes, close);
            return;
        } else if (a instanceof List) {
            diff((List<Object>) a, (List<Object>) b, path, changes, close);
            return;
        } else if (a instanceof Map) {
            diff((Map) a, (Map) b, path, changes, close);
            return;
        } else if (!ac.isAnnotationPresent(DetailDiff.class)) { // simple type
            changes.add(Change.changed(path, gson.toJson(b)));
            return;
        }

        if (!close.add(a)) {
            throw new IllegalArgumentException("recursive reference found for " + a);
        }

        try {
            for (Field field : Lambda.getPropertyFields(ac)) {
                if (field.isAnnotationPresent(ExcludeDiff.class)) continue;
                if (fieldInclusionStrategy != null && !fieldInclusionStrategy.test(field)) continue;

                diff(field.get(a), field.get(b), path + SEP + field.getName(), changes, close);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void diff(List<Object> a, List<Object> b, String path, List<Change> changes, Set<Object> close) {
        if (a == b) return;
        if (a == null ^ b == null) {
            changes.add(Change.changed(path, serializeObject(b)));
            return;
        }

        MatchResult join = innerJoin(a, b);
        int[] order = new int[b.size()];

        int deleted = 0;
        for (int i = 0; i < a.size(); i++) {
            Integer j = join.left.get(i);
            if (j == null) {
                changes.add(Change.removed(path, i - deleted++));
            } else {
                diff(a.get(i), b.get(j), path + SEP + (i - deleted), changes, close);
                order[i - deleted] = j;
            }
        }

        int len = a.size() - deleted;
        for (int j = 0; j < b.size(); j++) {
            if (!join.right.containsKey(j)) {
                order[len] = j;
                changes.add(Change.inserted(path, len, serializeObject(b.get(j))));
                len++;
            }
        }

        for (int i = 0; i < order.length - 1; i++) {
            for (int j = i + 1; j < order.length; j++) {
                if (order[i] > order[j]) {
                    int k = order[i];
                    order[i] = order[j];
                    order[j] = k;

                    changes.add(Change.swapped(path, i, j));
                }
            }
        }
    }

    private void diff(Map<Object, Object> a, Map<Object, Object> b, String path, List<Change> changes, Set<Object> close) {
        if (a == b) return;
        if (a == null ^ b == null) {
            changes.add(Change.changed(path, serializeObject(b)));
            return;
        }

        for (Object key : a.keySet()) {
            if (b.containsKey(key)) {
                diff(a.get(key), b.get(key), path + SEP + key, changes, close);
            } else {
                changes.add(Change.removed(path, key));
            }
        }

        for (Object key : b.keySet()) {
            if (!a.containsKey(key)) {
                changes.add(Change.inserted(path, key, serializeObject(b.get(key))));
            }
        }
    }

    private String serializeObject(Object o) {
        return gson.toJson(o);
    }
}
