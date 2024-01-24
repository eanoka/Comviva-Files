package com.grameenphone.wipro.fmfs.mfs_communicator.model.spdb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.grameenphone.wipro.utility.marshal.Xml;

import java.util.HashMap;
import java.util.Map;

@JacksonXmlRootElement(localName = "soapenv:Envelope")
public class SOAPEnvelopeRequest {
    public static class SOAPBody {
        @JacksonXmlProperty(localName = "ser:trans")
        public SOAPService trans = new SOAPService();
    }

    public static class SOAPService {
        private String arg0;
        private String arg1;

        public String getArg0() {
            return arg0;
        }

        public void setArg0(String arg0) {
            this.arg0 = arg0;
        }

        public String getArg1() {
            return arg1;
        }

        public void setArg1(String arg1) {
            this.arg1 = arg1;
        }

        public void setArg1(Object arg1) throws JsonProcessingException {
            this.arg1 = Xml.toXml(arg1);
        }
    }

    @JacksonXmlProperty(isAttribute = true, localName = "xmlns:soapenv")
    private String soapenv = "http://schemas.xmlsoap.org/soap/envelope/";
    @JacksonXmlProperty(isAttribute = true, localName = "xmlns:ser")
    private String ser = "http://service.ws.tangdi/";
    @JacksonXmlProperty(localName = "soapenv:Header")
    public Map header = new HashMap();
    @JacksonXmlProperty(localName = "soapenv:Body")
    public SOAPBody body = new SOAPBody();
}