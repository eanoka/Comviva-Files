package com.grameenphone.wipro.utility.common;

import java.util.ArrayList;
import java.util.List;

public class ListUtil {
	public static <T> List<T> collect(List thisArgs, Callback<T> filter) {
		List result = new ArrayList();
		for (Object arg : thisArgs) {
			result.add(filter.call(arg));
		}
		return result;
	}
}