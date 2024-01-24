package com.grameenphone.wipro.utility.marshal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import javax.xml.stream.XMLInputFactory;
import java.io.IOException;

public class Xml {
    private static XmlMapper mapper = new XmlMapper();

    static {
        mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES).disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.getFactory().getXMLInputFactory().setProperty(XMLInputFactory.SUPPORT_DTD, false);
    }

    public static <T> T fromXml(String source, Class<T> target) throws IOException {
        return mapper.readValue(source, target);
    }

    public static <T> String toXml(T source) throws JsonProcessingException {
        return mapper.writeValueAsString(source);
    }
}