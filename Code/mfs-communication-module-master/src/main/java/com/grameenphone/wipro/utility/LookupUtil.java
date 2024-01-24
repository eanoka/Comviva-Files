package com.grameenphone.wipro.utility;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Paths;

@SuppressWarnings("Duplicates")
public class LookupUtil {
	public static File lookupConfFile(String configFile) throws MalformedURLException, UnsupportedEncodingException {
		System.out.println("Started lookup " + configFile);
		File config = getFromCatalina(configFile);
		if(config.exists()) {
			return config;
		}
		config = getFromCurrentDir(configFile);
		if(config.exists()) {
			return config;
		}
		config = getFromClassLocationRecursive(configFile);
		if(config.exists()) {
			return config;
		}
		config = getFromUserRoot(configFile);
		if(config.exists()) {
			return config;
		}
		return null;
	}

	private static File getFromCatalina(String configFile) {
		String configLocation = System.getProperty("catalina.home") + File.separatorChar + "conf";
		System.out.println("Looking up in ${catelina home}/conf = " + configLocation);
		return new File(configLocation, configFile);
	}

	private static File getFromUserRoot(String configFile) {
		String configLocation = System.getProperty("user.home") + File.separatorChar + ".fmfs" + File.separatorChar + "conf";
		System.out.println("Looking up in ${user home}/conf = " + configLocation);
		return new File(configLocation, configFile);
	}

	private static File getFromCurrentDir(String configFile) {
		System.out.println("Looking up in ${current}/conf = " + new File("").getAbsolutePath());
		return new File("conf" + File.separatorChar + configFile);
	}

	private static File getFromClassLocationRecursive(String configFile) throws MalformedURLException, UnsupportedEncodingException {
		URL url = LookupUtil.class.getResource("LookupUtil.class");
		String classLocationDir;
		if(url.getProtocol().equals("jar")) {
			classLocationDir = new URL(url.getFile()).getFile();
			classLocationDir = classLocationDir.substring(0, classLocationDir.indexOf("jar!")) + "jar";
		} else {
			classLocationDir = url.getFile();
		}
		classLocationDir = URLDecoder.decode(classLocationDir, "UTF-8");
		File configDir = new File(classLocationDir);
		File config;
		do {
			System.out.println("Looking up in ${class root ancestor}/conf = " + configDir.getAbsolutePath());
			config = new File(configDir, "conf" + File.separatorChar + configFile);
			configDir = configDir.getParentFile();
		} while(configDir != null && !config.exists());
		return config;
	}
}