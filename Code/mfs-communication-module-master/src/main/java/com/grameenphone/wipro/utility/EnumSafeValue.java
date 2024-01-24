package com.grameenphone.wipro.utility;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class EnumSafeValue {
	private static Method constantDirectory;

	static {
		try {
			constantDirectory = Class.class.getDeclaredMethod("enumConstantDirectory");
			constantDirectory.setAccessible(true);
		} catch (NoSuchMethodException e) {
		}
	}

	public static <T extends Enum<T>> T valueOf(Class<T> enumType, String name) {
		try {
			Map<String, T> directory = (Map<String, T>)constantDirectory.invoke(enumType);
			return directory.get(name);
		} catch (IllegalAccessException | InvocationTargetException e) {
		}
		return null;
	}
}