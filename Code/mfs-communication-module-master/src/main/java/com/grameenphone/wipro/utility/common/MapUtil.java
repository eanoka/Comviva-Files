package com.grameenphone.wipro.utility.common;

import java.util.Map;
import java.util.regex.Pattern;

public class MapUtil {
	public static  Object getFirst(Map<String, ?> source, String pattern) {
		final Object[] found = new Object[1];
		try {
			if(source != null) {
				source.forEach((k, v) -> {
					if (Pattern.matches(pattern, k)) {
						found[0] = v;
						throw new Error(); //to break
					}
				});
			}
		} catch (Error p) {}
		return found[0];
	}
}