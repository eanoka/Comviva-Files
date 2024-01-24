package com.grameenphone.wipro.utility.common;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ListUtil {
	public static <T, U> List<T> collect(List<U> thisArgs, Function<U, T> filter) {
		List<T> result = new ArrayList();
		for (U arg : thisArgs) {
			result.add(filter.apply(arg));
		}
		return result;
	}
}