package com.grameenphone.wipro.utility.common;

public class NumberUtil {
    public static String getOrdinal(int number) {
        if(number > 10 && number < 14) {
            return "th";
        }
        switch(number % 10) {
            case 1:
                return "st";
            case 2:
                return "nd";
            case 3:
                return "rd";
            default:
                return "th";
        }
    }

    public static String toBdFormat(Number number) {
        if(number == null) {
            return "";
        }
        String fixed = String.format("%.2f", number.doubleValue());
        boolean hasMinus = false;
        if(fixed.startsWith("-")) {
            hasMinus = true;
            fixed = fixed.substring(1);
        }
        int total = fixed.length() - 6;
        String returnable = "";
        if(total <= 0) {
            return (hasMinus ? "-" : "") + fixed;
        }
        String last6 = fixed.substring(total);
        fixed = fixed.substring(0, total);
        if(total % 2 != 0) {
            returnable = fixed.charAt(0) + ",";
            fixed = fixed.substring(1);
        }
        for(int g=0; g<fixed.length();) {
            returnable += fixed.charAt(g++);
            returnable += fixed.charAt(g++);
            returnable += ",";
        }
        return (hasMinus ? "-" : "") + returnable + last6;
    }
}