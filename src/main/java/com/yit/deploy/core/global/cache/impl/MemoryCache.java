package com.yit.deploy.core.global.cache.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryCache extends Cache {

    /**
     * data. group/key -> cache item
     */
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, CacheItem>> data = new ConcurrentHashMap<>();

    /**
     * list stored group names
     */
    @Nonnull
    @Override
    protected Collection<String> listGroups() {
        return Collections.list(data.keys());
    }

    /**
     * list stored keys in a specific group
     *
     * @param group
     */
    @Nonnull
    @Override
    protected Collection<String> listKeys(@Nonnull String group) {
        ConcurrentHashMap<String, CacheItem> map = data.get(group);
        return map == null ? Collections.emptyList() : Collections.list(map.keys());
    }

    /**
     * store a cache item to cache media
     *
     * @param item the item to store
     */
    @Override
    protected void store(@Nonnull CacheItem item) {
        data.computeIfAbsent(item.group, x -> new ConcurrentHashMap<>()).put(item.key, item);
    }

    /**
     * remove a cache item by the specific group / key.
     *
     * @param group group of the item
     * @param key   key of the item
     */
    @Override
    protected void clean(@Nonnull String group, @Nonnull String key) {
        ConcurrentHashMap<String, CacheItem> map = data.get(group);
        if (map == null) return;
        map.remove(key);
    }

    /**
     * fetch a cache item from cache media stored by previous store call.
     *
     * @param group the group of the cache item
     * @param key   the key of the cache item, the key must be unique among one group
     * @return the fetched cache item
     */
    @Override
    @Nullable
    protected CacheItem fetch(@Nonnull String group, @Nonnull String key) {
        ConcurrentHashMap<String, CacheItem> map = data.get(group);
        if (map == null) return null;
        return map.get(key);
    }
}
