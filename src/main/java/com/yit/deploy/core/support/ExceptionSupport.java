package com.yit.deploy.core.support;

public interface ExceptionSupport {
    default <T, R> R unchecked(T obj, FunctionWithException<T, R> f) {
        try {
            return f.apply(obj);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    default <T> void unchecked(T obj, ActionWithException<T> f) {
        try {
            f.act(obj);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    interface FunctionWithException<T, R> {
        R apply(T obj) throws Exception;
    }

    interface ActionWithException<T> {
        void act(T obj) throws Exception;
    }
}
