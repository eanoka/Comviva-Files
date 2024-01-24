package com.grameenphone.wipro.fmfs.mfs_processor.test.config;

import com.grameenphone.wipro.fmfs.mfs_communicator.Application;
import com.grameenphone.wipro.spring.boot.extra.ExternalPropertySource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.context.MergedContextConfiguration;

import java.util.List;

public class TestContextLoader extends SpringBootContextLoader {
    @Override
    protected List<ApplicationContextInitializer<?>> getInitializers(MergedContextConfiguration config, SpringApplication application) {
        List<ApplicationContextInitializer<?>> initializers = super.getInitializers(config, application);
        initializers.add((applicationContext) -> {
            Application.context = applicationContext;
            Application.environment = (StandardEnvironment) applicationContext.getEnvironment();
            Application.environment.getPropertySources().addLast(new ExternalPropertySource());
        });
        return initializers;
    }
}