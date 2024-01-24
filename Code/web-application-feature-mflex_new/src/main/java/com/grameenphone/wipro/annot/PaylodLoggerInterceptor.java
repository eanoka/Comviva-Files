package com.grameenphone.wipro.annot;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.BiFunction;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PaylodLoggerInterceptor {
    String pattern();
    Class<? extends BiFunction<String, Boolean, String>> interceptor();
}