package com.yit.deploy.core.global.resource;

import com.yit.deploy.core.function.Action;
import com.yit.deploy.core.function.Int2Int;
import com.yit.deploy.core.function.IntegerHolder;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class ResourceCounter {
    private ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

    public int increment(String key) {
        return compute(key, c -> ++c);
    }

    public int decrement(String key) {
        return compute(key, c -> --c);
    }

    public int get(String key) {
        return compute(key, c -> c);
    }

    public boolean zero(String key) {
        return get(key) == 0;
    }

    private int compute(String key, Int2Int f) {
        IntegerHolder holder = new IntegerHolder();
        map.compute(key, (k, c) -> {
            holder.data = c = f.f(c == null ? 0 : c);
            return c == 0 ? null : c;
        });
        return holder.data;
    }

    public void using(String key, Action action) {
        increment(key);
        try {
            action.run();
        } finally {
            decrement(key);
        }
    }

    public <T> T using(String key, Supplier<T> supplier) {
        increment(key);
        try {
            return supplier.get();
        } finally {
            decrement(key);
        }
    }
}
