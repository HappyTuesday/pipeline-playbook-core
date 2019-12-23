package com.yit.deploy.core.global.resource;

import com.yit.deploy.core.function.Action;
import groovy.lang.Closure;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface ResourceSupport {

    /**
     * acquire a resource
     * @param key resource key
     */
    void acquire(@Nonnull String key);

    /**
     * try to acquire the resource, without waiting
     * @param key resource key
     * @return return true if it actually got the resource
     */
    boolean tryAcquire(@Nonnull String key);

    /**
     * release a resource
     * @param key resource key
     */
    void release(@Nonnull String key);

    /**
     * acquire a resource, execute the waitingCallback just before long waiting if the resource cannot be acquired immediately.
     * @param key resource key
     * @param callback called if we are going to wait
     */
    default void acquire(@Nonnull String key, @Nonnull Consumer<String> callback) {
        if (tryAcquire(key)) return;
        callback.accept(key);
        acquire(key);
    }

    /**
     * acquire the resource before execution of c and release the acquired resource after execution successfully or failed
     * @param key resource key
     * @param c closure to execute
     * @param <T> return type of the closure
     * @return
     */
    default <T> T using(@Nonnull String key, @Nonnull Closure<T> c) {
        acquire(key);
        try {
            return c.call();
        } finally {
            release(key);
        }
    }

    /**
     * acquire the resource before execution of c and release the acquired resource after execution successfully or failed
     * @param key resource key
     * @param a action to execute
     */
    default <T> T using(@Nonnull String key, @Nonnull Supplier<T> a) {
        acquire(key);
        try {
            return a.get();
        } finally {
            release(key);
        }
    }

    /**
     * acquire the resource before execution of c and release the acquired resource after execution successfully or failed
     * @param key resource key
     * @param a action to execute
     */
    default void using(@Nonnull String key, @Nonnull Action a) {
        acquire(key);
        try {
            a.run();
        } finally {
            release(key);
        }
    }

    /**
     * wrap the action so it will acquire the resource before execution of c and release the acquired resource after execution successfully or failed
     * @param key resource key
     * @param a action to execute
     */
    default Action wrap(@Nonnull String key, @Nonnull Action a) {
        return () -> using(key, a);
    }

    /**
     * acquire the resource before execution of c and release the acquired resource after execution successfully or failed
     * @param key resource key
     * @param action action to execute
     * @param callback execute if we are going to wait
     */
    default void using(@Nonnull String key, @Nonnull Action action, @Nonnull Consumer<String> callback) {
        acquire(key, callback);
        try {
            action.run();
        } finally {
            release(key);
        }
    }
}
