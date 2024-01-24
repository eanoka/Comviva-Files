package com.grameenphone.wipro.utility.common;

import com.grameenphone.wipro.utility.KV;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class MapUtil {
	public static  Object getFirstMatchingPattern(Map<String, ?> source, String pattern) {
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

	public static <A, B> Map<A, B> of(KV<A, B>... keyValues) {
		HashMap<A, B> returnMap = new LinkedHashMap<>();
		for(KV<A, B> keyValue : keyValues) {
			if(keyValue.key != null && keyValue.value != null) {
				returnMap.put(keyValue.key, keyValue.value);
			}
		}
		return returnMap;
	}
}