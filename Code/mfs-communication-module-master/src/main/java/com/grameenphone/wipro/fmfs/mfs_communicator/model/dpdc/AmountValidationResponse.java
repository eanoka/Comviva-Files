package com.grameenphone.wipro.fmfs.mfs_communicator.model.dpdc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.grameenphone.wipro.utility.marshal.Xml;

import java.io.IOException;

@JacksonXmlRootElement(localName = "Envelope")
public class AmountValidationResponse {
    @JacksonXmlProperty(localName = "Header")
    public SOAPHeader header = new SOAPHeader();
    @JacksonXmlProperty(localName = "Body")
    public SOAPBody body = new SOAPBody();

    public static class SOAPHeader {}

    public static class SOAPBody {
        @JacksonXmlProperty(localName = "dataValidationResponse")
        public CustomerInfoWithAmount response = new CustomerInfoWithAmount();
    }

    public static class CustomerInfoWithAmount {
        @JacksonXmlProperty(localName = "return")
        private String _return;
        @JsonIgnore
        public CustomerInfo customerInfo;
        @JsonIgnore
        public SoapErrorResponse error;

        private void set_return(String _return) {
            try {
                if (_return.startsWith("<error>")) {
                    this.error = Xml.fromXml(_return, SoapErrorResponse.class);
                } else {
                    this.customerInfo = Xml.fromXml(_return, CustomerInfo.class);
                }
            } catch (IOException e) {
            }
        }
    }
}