package com.yit.deploy.core.inherits;

import com.yit.deploy.core.collections.ReverseList;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class Inherits {

    private Inherits() {
    }

    public static <T> List<T> descendingParents(T self, Function<T, Iterator<T>> parents) {
        LinkedList<T> list = new LinkedList<>();
        travelParents(self, parents, new HashSet<>(), list::add);
        return list;
    }

    public static <T> void descending(T self, Function<T, Iterator<T>> parents, Consumer<T> visitor) {
        Set<T> close = new HashSet<>();
        close.add(self); // avoid visiting self in the parents
        travelParents(self, parents, close, visitor);
        visitor.accept(self);
    }

    public static <T> List<T> descending(T self, Function<T, Iterator<T>> parents) {
        LinkedList<T> list = new LinkedList<>();
        Set<T> close = new HashSet<>();
        close.add(self); // avoid visiting self in the parents
        travelParents(self, parents, close, list::add);
        list.add(self);
        return list;
    }

    public static <T> List<T> descendingMany(List<T> self, Function<T, Iterator<T>> parents) {
        LinkedList<T> list = new LinkedList<>();
        Set<T> close = new HashSet<>();
        for (T t : self) {
            if (close.add(t)) { // avoid visiting self in the parents
                travelParents(t, parents, close, list::add);
                list.add(t);
            }
        }
        return list;
    }

    public static <T> List<T> ascendingParents(T self, Function<T, Iterator<T>> parents) {
        return new ReverseList<>(descendingParents(self, parents));
    }

    public static <T> List<T> ascending(T self, Function<T, Iterator<T>> parents) {
        return new ReverseList<>(descending(self, parents));
    }

    public static <T> boolean all(T self, Function<T, Iterator<T>> parents, Predicate<T> p) {
        for (T t : descending(self, parents)) {
            if (!p.test(t)) {
                return false;
            }
        }
        return true;
    }

    public static <T> boolean any(T self, Function<T, Iterator<T>> parents, Predicate<T> p) {
        for (T t : ascending(self, parents)) {
            if (p.test(t)) {
                return true;
            }
        }
        return false;
    }

    public static <T, R> R nearestInParents(T self, Function<T, Iterator<T>> parents, Function<T, R> f) {
        for (T t : ascendingParents(self, parents)) {
            R r = f.apply(t);
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    public static <T, R> R nearest(T self, Function<T, Iterator<T>> parents, Function<T, R> f) {
        for (T t : ascending(self, parents)) {
            R r = f.apply(t);
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    private static <T> void travelParents(T self, Function<T, Iterator<T>> parents, Set<T> close, Consumer<T> visitor) {
        for (Iterator<T> iter = parents.apply(self); iter.hasNext();) {
            T t = iter.next();
            if (close.add(t)) {
                travelParents(t, parents, close, visitor);
                visitor.accept(t);
            }
        }
    }

    public static <T> boolean belongsTo(T self, Function<T, Iterator<T>> parents, T parent) {
        if (self.equals(parent)) {
            return true;
        }
        for (T t : descending(self, parents)) {
            if (t.equals(parent)) {
                return true;
            }
        }
        return false;
    }
}
