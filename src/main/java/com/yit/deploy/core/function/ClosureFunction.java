package com.yit.deploy.core.function;

@FunctionalInterface
public interface ClosureFunction<OWNER, DELEGATE, RESULT> {
    RESULT apply(OWNER owner, DELEGATE delegate, Object ... args);
}
