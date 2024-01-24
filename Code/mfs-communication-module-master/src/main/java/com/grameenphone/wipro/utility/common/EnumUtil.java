package com.grameenphone.wipro.utility.common;

public class EnumUtil {
    public static <T extends Enum> boolean hasValue(Class<T> enumClass, String valueToCheck) {
        try {
            Enum.valueOf(enumClass, valueToCheck);
            return true;
        } catch(Exception h) {
            return false;
        }
    }
}