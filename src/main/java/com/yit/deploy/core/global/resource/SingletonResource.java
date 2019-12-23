package com.yit.deploy.core.global.resource;

import javax.annotation.Nonnull;
import java.util.concurrent.locks.ReentrantLock;

/**
 * resource only be acquired once before waiting
 */
public class SingletonResource extends Resource<ReentrantLock> implements ResourceSupport {

    @Nonnull
    @Override
    protected ReentrantLock createLock(@Nonnull String key) {
        return new ReentrantLock();
    }

    /**
     * acquire a resource
     *
     * @param key resource key
     */
    @Override
    public void acquire(@Nonnull String key) {
        acquireInternal(key, ReentrantLock::lockInterruptibly);
    }

    /**
     * try to acquire the resource, without waiting
     *
     * @param key resource key
     * @return return true if it actually got the resource
     */
    @Override
    public boolean tryAcquire(@Nonnull String key) {
        return tryAcquireInternal(key, ReentrantLock::tryLock);
    }

    /**
     * release a resource
     *
     * @param key resource key
     */
    @Override
    public void release(@Nonnull String key) {
        releaseInternal(key, ReentrantLock::unlock);
    }
}
