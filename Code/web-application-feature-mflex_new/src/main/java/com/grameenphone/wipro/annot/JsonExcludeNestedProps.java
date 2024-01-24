package com.grameenphone.wipro.annot;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * To exclude nested json paths from serialization. e.g for <br> class A { B b; int h = 90; }, class B { C c; int i = 5; }, class C { int d; } <br> value be "b.c" <br> then serialization of A e = new A(); will be {"h": 90, "b": {"i": 5} } // c from b is excluded
 * <br><br>
 * However to make it effective you have to register {@link com.grameenphone.wipro.extensions.jackson.JsonPropertyExcludableMapperModule} module to your json {@link com.fasterxml.jackson.databind.ObjectMapper}
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonExcludeNestedProps {
    String[] value() default {};
}