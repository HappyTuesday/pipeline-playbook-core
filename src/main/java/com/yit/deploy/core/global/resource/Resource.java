package com.yit.deploy.core.global.resource;

import com.yit.deploy.core.function.BooleanHolder;

import javax.annotation.Nonnull;
import java.util.concurrent.*;
import java.util.logging.Logger;

public abstract class Resource<T> {

    private static Logger logger = Logger.getLogger(Resource.class.getName());
    /**
     * each resource key has a lock
     */
    private final ConcurrentHashMap<String, ResourceBlock<T>> blocks = new ConcurrentHashMap<>();

    /**
     * get the lock of a certain resource key for acquire
     * @param key resource key
     * @param proxy define how to acquire
     */
    protected void acquireInternal(@Nonnull String key, AcquireOperator<T> proxy) {

        ResourceBlock<T> block = blocks.compute(key, (k, old) -> {
            ResourceBlock<T> b = old == null ? resourceBlock(k) : old;
            b.count++;
            return b;
        });

        try {
            proxy.acquire(block.lock);
        } catch (InterruptedException e) {
            blocks.computeIfPresent(key, (k, b) -> {
                b.count--;
                return b;
            });
            throw new RuntimeException(e);
        }
    }

    /**
     * try to get the lock of a certain resource key for acquire
     * @param key resource key
     * @param proxy define how to acquire
     */
    protected boolean tryAcquireInternal(@Nonnull String key, TryAcquireOperator<T> proxy) {

        BooleanHolder result = new BooleanHolder();
        blocks.compute(key, (k, old) -> {
            ResourceBlock<T> b = old == null ? resourceBlock(k) : old;
            if (proxy.tryAcquire(b.lock)) {
                b.count++;
                result.value = true;
            }
            return b;
        });

        return result.value;
    }

    /**
     * get the lock of a certain resource key for release
     * @param key resource key
     * @param proxy define how to release
     */
    protected void releaseInternal(@Nonnull String key, ReleaseOperator<T> proxy) {
        blocks.compute(key, (k, b) -> {
            if (b == null) {
                throw new IllegalStateException("acquire & release does not balance");
            }

            proxy.release(b.lock);
            b.count--;

            if (b.count == 0) {
                // logger.info("reclaim resource " + key + " created by thread " + b.owner);
                return null; // remove the block if no one uses it
            }

            return b;
        });
    }

    private ResourceBlock<T> resourceBlock(String key) {
        return new ResourceBlock<>(key, createLock(key), Thread.currentThread().getName());
    }

    /**
     * create a new lock instance with for the resource key
     * @param key resource key
     * @return new created lock instance
     */
    @Nonnull
    protected abstract T createLock(@Nonnull String key);

    @FunctionalInterface
    public interface AcquireOperator<T> {
        void acquire(T lock) throws InterruptedException;
    }

    @FunctionalInterface
    public interface ReleaseOperator<T> {
        void release(T lock);
    }

    @FunctionalInterface
    public interface TryAcquireOperator<T> {
        boolean tryAcquire(T lock);
    }

    private static class ResourceBlock<T> {
        String key;
        T lock;
        int count;
        String owner;

        ResourceBlock(String key, T lock, String owner) {
            this.key = key;
            this.lock =lock;
            this.owner = owner;
        }
    }
}
