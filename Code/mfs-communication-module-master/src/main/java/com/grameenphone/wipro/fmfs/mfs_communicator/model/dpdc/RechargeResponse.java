package com.grameenphone.wipro.fmfs.mfs_communicator.model.dpdc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.grameenphone.wipro.utility.marshal.Xml;

import java.io.IOException;

@JacksonXmlRootElement(localName = "Envelope")
public class RechargeResponse {
    @JacksonXmlProperty(localName = "Header")
    public SOAPHeader header = new SOAPHeader();
    @JacksonXmlProperty(localName = "Body")
    public SOAPBody body = new SOAPBody();

    public static class SOAPHeader {
    }

    public static class SOAPBody {
        @JacksonXmlProperty(localName = "createRechargeResponse")
        public RechargeWMsgResponse response = new RechargeWMsgResponse();
    }

    public static class RechargeWMsgResponse {
        @JsonIgnore
        public DpdcVendingDetail vending;
        @JsonIgnore
        public SoapErrorResponse error;
        @JsonIgnore
        public SoapDisputeResponse message;

        @JacksonXmlProperty(localName = "return")
        private String _return;

        private void set_return(String _return) {
            try {
                if (_return.startsWith("<error>")) {
                    this.error = Xml.fromXml(_return, SoapErrorResponse.class);
                } else if (_return.startsWith("<message>")) {
                    this.message = Xml.fromXml(_return, SoapDisputeResponse.class);
                } else {
                    this.vending = Xml.fromXml(_return, DpdcVendingDetail.class);
                }
            } catch (IOException e) {
            }
        }
    }
}