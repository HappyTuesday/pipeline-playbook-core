package com.yit.deploy.core.global.resource;

import javax.annotation.Nonnull;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RWResource extends Resource<ReentrantReadWriteLock> {

    public final ResourceSupport read = new ResourceReadLock();

    public final ResourceSupport write = new ResourceWriteLock();

    @Nonnull
    @Override
    protected ReentrantReadWriteLock createLock(@Nonnull String key) {
        return new ReentrantReadWriteLock();
    }

    private class ResourceReadLock implements ResourceSupport {
        /**
         * acquire a resource
         *
         * @param key resource key
         */
        @Override
        public void acquire(@Nonnull String key) {
            acquireInternal(key, l -> l.readLock().lockInterruptibly());
        }

        /**
         * try to acquire the resource, without waiting
         *
         * @param key resource key
         * @return return true if it actually got the resource
         */
        @Override
        public boolean tryAcquire(@Nonnull String key) {
            return tryAcquireInternal(key, l -> l.readLock().tryLock());
        }

        /**
         * release a resource
         *
         * @param key resource key
         */
        @Override
        public void release(@Nonnull String key) {
            releaseInternal(key, l -> l.readLock().unlock());
        }
    }

    private class ResourceWriteLock implements ResourceSupport {
        /**
         * acquire a resource
         *
         * @param key resource key
         */
        @Override
        public void acquire(@Nonnull String key) {
            acquireInternal(key, l -> l.writeLock().lockInterruptibly());
        }

        /**
         * try to acquire the resource, without waiting
         *
         * @param key resource key
         * @return return true if it actually got the resource
         */
        @Override
        public boolean tryAcquire(@Nonnull String key) {
            return tryAcquireInternal(key, l -> l.writeLock().tryLock());
        }

        /**
         * release a resource
         *
         * @param key resource key
         */
        @Override
        public void release(@Nonnull String key) {
            releaseInternal(key, l -> l.writeLock().unlock());
        }
    }
}
