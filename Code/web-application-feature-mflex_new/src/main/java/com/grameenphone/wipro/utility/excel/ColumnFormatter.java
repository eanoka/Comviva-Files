package com.grameenphone.wipro.utility.excel;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.function.Function;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target({FIELD, TYPE})
public @interface ColumnFormatter {
    Class<Function<Object, String>> formatter();
}