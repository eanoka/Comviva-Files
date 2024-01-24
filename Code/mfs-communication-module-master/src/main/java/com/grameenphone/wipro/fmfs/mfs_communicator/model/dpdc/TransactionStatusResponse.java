package com.grameenphone.wipro.fmfs.mfs_communicator.model.dpdc;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.dpdc.RechargeResponse.RechargeWMsgResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.dpdc.RechargeResponse.SOAPBody;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.dpdc.RechargeResponse.SOAPHeader;
import com.grameenphone.wipro.utility.marshal.Xml;

@JacksonXmlRootElement(localName = "Envelope")
public class TransactionStatusResponse {
	
    @JacksonXmlProperty(localName = "Body")
    public SOAPBody body = new SOAPBody();

    public static class SOAPBody {
        @JacksonXmlProperty(localName = "checkTransactionResponse")
        public TransactionResponse response = new TransactionResponse();
    }
    
    public static class TransactionResponse {
        @JsonIgnore
        public DpdcVendingDetail vending;
        @JsonIgnore
        public SoapErrorResponse error;
        @JsonIgnore
        public OrderCancelResponse ordercancel;

        @JacksonXmlProperty(localName = "return")
        private String _return;

        private void set_return(String _return) {
            try {
                if (_return.startsWith("<error>")) {
                    this.error = Xml.fromXml(_return, SoapErrorResponse.class);
                } 
                else if (_return.startsWith("<OrderCancel>")) {
                    this.ordercancel = Xml.fromXml(_return, OrderCancelResponse.class);
                } 
                else {
                    this.vending = Xml.fromXml(_return, DpdcVendingDetail.class);
                }
            } catch (IOException e) {
            }
        }
    }
}
