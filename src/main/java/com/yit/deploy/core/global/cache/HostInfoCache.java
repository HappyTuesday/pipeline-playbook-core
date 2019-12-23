package com.yit.deploy.core.global.cache;

import com.yit.deploy.core.global.cache.impl.Cache;
import com.yit.deploy.core.global.cache.impl.MemoryCache;

import java.util.function.Function;

public class HostInfoCache {
    private static final String CACHE_GROUP = "host-info";
    private static final int SECONDS_TO_LIVE = 24 * 60 * 60;

    private static final Cache cache = new MemoryCache();

    public static String get(String host, String type, Function<String, String> fetcher) {
        return cache.get(CACHE_GROUP, host + "/" + type, fetcher, SECONDS_TO_LIVE);
    }
}
