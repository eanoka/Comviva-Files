package com.grameenphone.wipro.spring.boot.extra;

import java.io.FileInputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import com.grameenphone.wipro.utility.LookupUtil;
import org.springframework.core.env.MapPropertySource;

/**
 * To use external ntmc.properties as property source
 */
public class ExternalPropertySource extends MapPropertySource {
    public final static String CONFIG_SOURCE_ID = "external_config_source";
    private final static Map<String, Object> propCache = new LinkedHashMap<>();

    public ExternalPropertySource() {
        super(CONFIG_SOURCE_ID, propCache);
        update();
    }

    private Properties loadConfigProps() {
        try {
            Properties ppt = new Properties();
            String profile = System.getProperty("execution.profile");
            if("test".equals(profile)) {
                ppt.load(new FileInputStream(LookupUtil.lookupConfFile(("communicator.properties.sample"))));
                ppt.load(new FileInputStream(LookupUtil.lookupConfFile(("test.communicator.properties"))));
            } else {
                ppt.load(new FileInputStream(LookupUtil.lookupConfFile("communicator.properties")));
            }
            return ppt;
        } catch (Exception e) {
            throw new RuntimeException("Configuration file could not be loaded");
        }
    }

    public void update() {
        if(this.source.size() == 0) {
            this.source.putAll(new LinkedHashMap(loadConfigProps()));
        }
    }

    public Object getProperty(String name, Object defaultValue) {
        Object value = super.getProperty(name);
        if(value == null) {
            value = defaultValue;
        }
        return value;
    }
}