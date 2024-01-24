package com.grameenphone.wipro.utility.common;

public class MedleyUtil {
	public static long getLastPageOffset(long total, int limit) {
		if(total == 0) {
			return 0;
		}
		return (total - 1) / limit * limit;
	}
}