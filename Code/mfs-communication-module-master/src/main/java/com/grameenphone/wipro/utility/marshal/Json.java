package com.grameenphone.wipro.utility.marshal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * @author wipro.zobair
 * @updated 23-11-21
 */
public class Json {
    public static ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.registerModule(new JavaTimeModule()).enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES).disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).enable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS.mappedFeature()).setDateFormat(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")).setTimeZone(TimeZone.getDefault());
    }

    public static <T> T fromJson(String source, Class<T> target) throws IOException {
        return fromJson(mapper, source, target);
    }

    public static <T> T fromJson(ObjectMapper mapper, String source, Class<T> target) throws IOException {
        return (mapper == null ? Json.mapper : mapper).readValue(source, target);
    }

    public static <T> T fromJson(String source, TypeReference<T> target) throws IOException {
        return mapper.readValue(source, target);
    }

    public static <T> String toJson(T source) throws JsonProcessingException {
        return mapper.writeValueAsString(source);
    }

    public static ObjectMapper mapper() {
        return mapper.copy();
    }
}