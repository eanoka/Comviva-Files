package com.grameenphone.wipro.extensions.spring.boot.extra;

import com.grameenphone.wipro.fmfs.cbp.Application;
import org.springframework.cache.Cache;
import org.springframework.core.io.Resource;

import java.util.concurrent.Callable;

/**
 * To cache a specific resource against a path
 */
public class ServletResourceCache implements Cache {
    Resource resource;

    public ServletResourceCache(String path) {
        resource = Application.context.getResource(path);
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Object getNativeCache() {
        return null;
    }

    @Override
    public ValueWrapper get(Object key) {
        return null;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        return (T)resource;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        return null;
    }

    @Override
    public void put(Object key, Object value) {}

    @Override
    public void evict(Object key) {}

    @Override
    public void clear() {}
}