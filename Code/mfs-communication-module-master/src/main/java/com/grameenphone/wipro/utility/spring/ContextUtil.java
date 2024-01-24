package com.grameenphone.wipro.utility.spring;

import com.grameenphone.wipro.fmfs.mfs_communicator.Application;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

public class ContextUtil {
    public static <T> T getBean(String beanName, Class<T> beanClass) {
        try {
            return Application.context.getBean(beanName, beanClass);
        } catch (NoSuchBeanDefinitionException nsb) {
        }
        return null;
    }

    public static <T> T getBean(Class<T> beanClass) {
        try {
            return Application.context.getBean(beanClass);
        } catch (NoSuchBeanDefinitionException nsb) {
        }
        return null;
    }
}