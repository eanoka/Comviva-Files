package com.grameenphone.wipro.fmfs.mfs_communicator.model.dpdc;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.grameenphone.wipro.fmfs.mfs_communicator.Application;

@JacksonXmlRootElement(localName = "soap:Envelope")
public class SOAPEnvelopeRequest<T> {
    @JacksonXmlProperty(localName = "soap:Header")
    public SOAPHeader header = new SOAPHeader();
    @JacksonXmlProperty(localName = "soap:Body")
    public T body;
    @JacksonXmlProperty(isAttribute = true, localName = "xmlns:soap")
    private String s = "http://schemas.xmlsoap.org/soap/envelope/";

    private static String username;
    private static String password;
    private static String posId;

    static {
        username = Application.environment.getProperty("dpdc_user_name");
        password = Application.environment.getProperty("dpdc_password");
        posId = Application.environment.getProperty("dpdc_posid");
    }

    public static class Authentication {
        @JacksonXmlProperty
        private String POSID = posId;
        @JacksonXmlProperty
        private String UserName = username;
        @JacksonXmlProperty
        private String PassWord = password;
    }

    public static class SOAPHeader {
        @JacksonXmlProperty(localName = "Authentication")
        public Authentication authentication = new Authentication();
    }
}