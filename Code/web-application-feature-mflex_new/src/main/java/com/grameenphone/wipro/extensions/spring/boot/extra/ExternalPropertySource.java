package com.grameenphone.wipro.extensions.spring.boot.extra;

import com.grameenphone.wipro.utility.LookupUtil;
import org.springframework.core.env.MapPropertySource;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * To use external bulkpay.properties as property source
 */
public class ExternalPropertySource extends MapPropertySource {
	public final static String CONFIG_SOURCE_ID = "external_config_source";
	private static ExternalPropertySource LAST_INSTANCE;

	public ExternalPropertySource() {
		super(CONFIG_SOURCE_ID, new LinkedHashMap<>());
		update();
		LAST_INSTANCE = this;
	}

	public static Properties loadConfigProps() {
		try {
			Properties ppt = new Properties();
			ppt.load(new FileInputStream(LookupUtil.lookupConfFile("bulkpay.properties")));
			return ppt;
		} catch (Exception e) {
			throw new RuntimeException("Configuration file could not be loaded", e);
		}
	}

	public void update() {
		this.source.putAll(new LinkedHashMap(loadConfigProps()));
	}

	public static Map<String, String> getFrontendProperties() {
		HashMap<String, String> map = new HashMap<>();
		for(String name : LAST_INSTANCE.getPropertyNames()) {
			if(name.startsWith("front.")) {
				map.put(name.substring(6), (String)LAST_INSTANCE.getProperty(name));
			}
		}
		return map;
	}
}