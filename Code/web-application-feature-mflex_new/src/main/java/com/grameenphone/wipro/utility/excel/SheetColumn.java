package com.grameenphone.wipro.utility.excel;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.function.Function;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target({FIELD, TYPE})
public @interface SheetColumn {
    String label() default "";
    int width() default 0;
    int order() default 0;
    String datePattern() default "";
}