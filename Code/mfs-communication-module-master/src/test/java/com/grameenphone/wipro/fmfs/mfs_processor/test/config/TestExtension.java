package com.grameenphone.wipro.fmfs.mfs_processor.test.config;

import org.springframework.test.context.junit.jupiter.SpringExtension;

public class TestExtension extends SpringExtension {
    static {
        System.setProperty("execution.profile", "test");
    }
}