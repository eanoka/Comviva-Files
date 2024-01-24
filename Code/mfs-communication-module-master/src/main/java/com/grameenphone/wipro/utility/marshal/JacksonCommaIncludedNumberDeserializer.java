package com.grameenphone.wipro.utility.marshal;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class JacksonCommaIncludedNumberDeserializer extends JsonDeserializer<Double> {
    @Override
    public Double deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String floatString = parser.getText();
        if (floatString.contains(",")) {
            floatString = floatString.replace(",", "");
        }
        return Double.valueOf(floatString);
    }
}
