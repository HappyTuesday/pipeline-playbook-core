package com.yit.deploy.core.function;

@FunctionalInterface
public interface FieldSetter<T, F> {
    void set(T target, F value);
}
