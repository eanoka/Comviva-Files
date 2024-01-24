package com.grameenphone.wipro.fmfs.cbp.controller;

import com.grameenphone.wipro.extensions.spring.boot.extra.ExternalPropertySource;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AppController {
    public Map<String, String> getFrontendProperties() {
        return ExternalPropertySource.getFrontendProperties();
    }
}