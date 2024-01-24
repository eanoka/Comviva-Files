package com.grameenphone.wipro.utility.common;

import java.util.function.BiFunction;

public class JsonPayloadPinMasker implements BiFunction<String, Boolean, String> {
    @Override
    public String apply(String payload, Boolean isRequest) {
        return isRequest ? payload.replaceAll("(\"pin\")\\s*:\\s*\"[^\"]*\"", "$1: \"****\"") : payload;
    }
}