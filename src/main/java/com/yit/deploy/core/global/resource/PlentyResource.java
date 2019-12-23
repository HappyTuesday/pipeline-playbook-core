package com.yit.deploy.core.global.resource;

import javax.annotation.Nonnull;
import java.util.concurrent.Semaphore;

/**
 * resource can be acquired up to fixed times before waiting
 */
public class PlentyResource extends Resource<Semaphore> implements ResourceSupport {
    /**
     * up to this limit, the resource can be acquired before waiting
     * @return
     */
    private final int limit;

    public PlentyResource(int limit) {
        this.limit = limit;
    }

    @Nonnull
    @Override
    protected Semaphore createLock(@Nonnull String key) {
        return new Semaphore(limit);
    }

    /**
     * acquire a resource
     *
     * @param key resource key
     */
    @Override
    public void acquire(@Nonnull String key) {
        acquireInternal(key, Semaphore::acquire);
    }

    /**
     * try to acquire the resource, without waiting
     *
     * @param key resource key
     * @return return true if it actually got the resource
     */
    @Override
    public boolean tryAcquire(@Nonnull String key) {
        return tryAcquireInternal(key, Semaphore::tryAcquire);
    }

    /**
     * release a resource
     *
     * @param key resource key
     */
    @Override
    public void release(@Nonnull String key) {
        releaseInternal(key, Semaphore::release);
    }
}
