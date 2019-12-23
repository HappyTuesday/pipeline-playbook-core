package com.yit.deploy.core.global.cache.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.logging.Level;

public abstract class Cache {
    private static Logger logger = Logger.getLogger(Cache.class.getName());

    protected final ScheduledExecutorService scheduler;

    public Cache() {
        scheduler = Executors.newSingleThreadScheduledExecutor(this::createCleanerThread);
        int interval = getCleanExpiredInterval();
        scheduler.scheduleWithFixedDelay(this::cleanExpiredCacheItems, interval, interval, TimeUnit.SECONDS);
    }

    private Thread createCleanerThread(Runnable r) {
        Thread t = new Thread(r);
        t.setName("cache-cleaner-scheduler");
        t.setDaemon(true);
        return t;
    }

    /**
     * override this method to provide its own clean up implementation.
     */
    protected void cleanExpiredCacheItems() {
        long now = new Date().getTime();
        int count = 0;
        for (String group : listGroups()) {
            for (String key : listKeys(group)) {
                try {
                    CacheItem item = fetch(group, key);
                    if (item != null && checkIfExpired(item, now)) {
                        clean(group, key);
                        count++;
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "failed to fetch or clean cache item " + group + "/" + key, e);
                }
            }
        }

        logger.log(Level.INFO, "" + count + " expired cache items have been cleaned up");
    }

    /**
     * the interval between two cleanExpiredCacheItems' call, in seconds
     * @return
     */
    protected int getCleanExpiredInterval() {
        return 60 * 60;
    }

    /**
     * list stored group names
     */
    @Nonnull
    protected abstract Collection<String> listGroups();

    /**
     * list stored keys in a specific group
     */
    @Nonnull
    protected abstract Collection<String> listKeys(@Nonnull String group);

    /**
     * store a cache item to cache media
     * @param item the item to store
     */
    protected abstract void store(@Nonnull CacheItem item) throws Exception;

    /**
     * remove a cache item by the specific group / key.
     * @param group group of the item
     * @param key key of the item
     */
    protected abstract void clean(@Nonnull String group, @Nonnull String key) throws Exception;

    /**
     * fetch a cache item from cache media stored by previous store call.
     * @param group the group of the cache item
     * @param key the key of the cache item, the key must be unique among one group
     * @return the fetched cache item
     */
    @Nullable
    protected abstract CacheItem fetch(@Nonnull String group, @Nonnull String key) throws Exception;

    public <T> T get(@Nonnull String group, @Nonnull String key, @Nonnull Function<String, T> producer, int secondsToLive) {
        CacheItem item;
        try {
            item = fetch(group, key);
        } catch (Exception e) {
            logger.log(Level.WARNING, "fetch value for cache " + key + " failed. producer will be called to get value for it.");
            item = null;
        }
        long now = new Date().getTime();
        if (item == null || checkIfExpired(item, now)) {
            // expired
            CacheItem newItem = new CacheItem();
            newItem.group = group;
            newItem.key = key;
            newItem.expiredTime = secondsToLive < 0 ? null : now + secondsToLive * 1000;
            // logger.log(Level.INFO, "populate cache value " + group + "/" + key);
            try {
                newItem.value = producer.apply(key);
            } catch (Exception e) {
                if (item == null) {
                    throw e;
                } else {
                    logger.log(Level.SEVERE, "populate new value for cache " + key + " failed. we will use old value for it", e);
                    newItem.value = item.value;
                }
            }

            try {
                store(newItem);
            } catch (Exception e) {
                logger.log(Level.WARNING, "save new value for cache " + key + " failed. new value will not been seen at next getting.", e);
            }
            item = newItem;
        }

        @SuppressWarnings("unchecked")
        T finalValue = (T) item.value;
        return finalValue;
    }

    protected boolean checkIfExpired(CacheItem item, long now) {
        return item.expiredTime != null && item.expiredTime < now;
    }

    protected static class CacheItem {
        public String group;
        public String key;
        public Object value;
        public Long expiredTime;
    }
}
