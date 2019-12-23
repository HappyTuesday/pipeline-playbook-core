package com.yit.deploy.core.function;

@FunctionalInterface
public interface FieldGetter<T, F> {
    F get(T target);
}
