package com.grameenphone.wipro.fmfs.mfs_communicator.model.spdb;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.grameenphone.wipro.utility.marshal.Xml;

import java.io.IOException;

@JacksonXmlRootElement(localName = "Envelope")
public class SOAPEnvelopeResponse {
    public class SOAPBody {
        public SOAPService transResponse = new SOAPService();

        public class SOAPService {
            @JacksonXmlProperty(localName = "return")
            private String returnParam;
        }
    }

    public <T> T getReturn(Class<T> returnType) throws IOException {
        return Xml.fromXml(Body.transResponse.returnParam, returnType);
    }

    public SOAPBody Body = new SOAPBody();
}