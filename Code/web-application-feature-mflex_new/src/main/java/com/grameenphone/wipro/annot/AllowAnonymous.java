package com.grameenphone.wipro.annot;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If any controller or action be annotated with this, then access restriction will be bypassed for that controller or actions
 * This is made effective with the help of {@link com.grameenphone.wipro.extensions.spring.boot.beans.config.RequestMappingHandlerMapping} and {@link com.grameenphone.wipro.fmfs.cbp.filter.SAMLLoginFilter}
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowAnonymous {
}