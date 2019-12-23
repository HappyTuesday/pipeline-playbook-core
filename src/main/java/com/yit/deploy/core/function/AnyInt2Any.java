package com.yit.deploy.core.function;

@FunctionalInterface
public interface AnyInt2Any<T, R> {
    R apply(T t, int i);
}
