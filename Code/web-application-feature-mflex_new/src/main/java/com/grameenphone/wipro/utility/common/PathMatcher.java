package com.grameenphone.wipro.utility.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.AntPathMatcher;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class PathMatcher extends AntPathMatcher {
    private static class CachedUrlMatcher {
        private final static HashMap<String, HashMap<String, Boolean>> cache = new LinkedHashMap<>(); //PATH: Pattern: true/false

        public static Boolean getMatch(String path, String pattern) {
            HashMap<String, Boolean> patternCache = cache.get(path);
            if(patternCache != null) {
                return patternCache.get(pattern);
            }
            return null;
        }

        public static void cacheMatch(String path, String pattern, boolean match) {
            HashMap<String, Boolean> patternCache = cache.get(path);
            if(patternCache == null) {
                cache.put(path, patternCache = new LinkedHashMap<>());
            }
            patternCache.put(pattern, match);
        }
    }

    private String method;
    private String pattern;

    public PathMatcher(String pattern) {
        super();
        setCachePatterns(true);
        this.pattern = pattern;
    }

    public PathMatcher(String urlPattern, String method) {
        this(urlPattern);
        this.method = method;
    }

    public boolean matches(HttpServletRequest request) {
        if(method != null && !request.getMethod().equals(method)) {
            return false;
        }
        String path = request.getRequestURI();
        if(!"/".equals(request.getContextPath())) {
            path = path.substring(request.getContextPath().length());
        }
        return match(path);
    }

    public boolean match(String path) {
        Boolean match = CachedUrlMatcher.getMatch(path, pattern);
        if(match == null) {
            match = match(pattern, path);
            CachedUrlMatcher.cacheMatch(path, pattern, match);
        }
        return match;
    }
}