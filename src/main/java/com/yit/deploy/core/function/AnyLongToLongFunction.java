package com.yit.deploy.core.function;

@FunctionalInterface
public interface AnyLongToLongFunction<T> {
    long apply(T e, long i);
}