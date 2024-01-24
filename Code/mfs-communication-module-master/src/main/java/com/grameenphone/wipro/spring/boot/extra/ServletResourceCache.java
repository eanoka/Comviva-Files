package com.grameenphone.wipro.spring.boot.extra;

import org.springframework.cache.Cache;
import org.springframework.core.io.Resource;

import java.util.concurrent.Callable;
import com.grameenphone.wipro.fmfs.mfs_communicator.Application;

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
    public void put(Object key, Object value) {

    }

    @Override
    public void evict(Object key) {

    }

    @Override
    public void clear() {

    }
}