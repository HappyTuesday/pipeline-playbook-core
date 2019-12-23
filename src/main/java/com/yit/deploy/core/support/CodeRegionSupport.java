package com.yit.deploy.core.support;

import groovy.lang.Closure;

public interface CodeRegionSupport {
    default void region(String comment, Closure body) {
        body.call();
    }

    default void region(Closure body) {
        region(null, body);
    }
}
