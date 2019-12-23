package com.yit.deploy.core.variables;

import java.util.function.Function;

public interface CacheProvider {
    <T> T withCache(String key, Function<String, T> provider);
}
