package com.yit.deploy.core.dsl.support;

import com.yit.deploy.core.variables.CacheProvider;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class MemoryCacheProvider implements CacheProvider {

    private final ConcurrentHashMap<String, Object> data = new ConcurrentHashMap<>();

    @Override
    public <T> T withCache(String key, Function<String, T> provider) {
        //noinspection unchecked
        return (T) data.computeIfAbsent(key, provider);
    }
}
