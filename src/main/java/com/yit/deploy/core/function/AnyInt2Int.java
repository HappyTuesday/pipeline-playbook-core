package com.yit.deploy.core.function;

@FunctionalInterface
public interface AnyInt2Int<T> {
    int apply(T t, int n);
}
