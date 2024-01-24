package com.grameenphone.wipro.fmfs.mfs_communicator.filter;

import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PayloadLoggerInterceptorRegister {
    private static List<String> patterns = new ArrayList<>();
    private static AntPathMatcher pathMatcher = new AntPathMatcher();

    public static void registerInterceptorFor(Collection<String> patterns) {
        PayloadLoggerInterceptorRegister.patterns.addAll(patterns);
    }

    public static boolean isMatch(String urlPath) {
        for (String g : patterns) {
            if(pathMatcher.match(g, urlPath)) {
                return true;
            }
        }
        return false;
    }
}