package com.grameenphone.wipro.fmfs.mfs_communicator.model.dpdc;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.dpdc.TransactionStatusRequest.SOAPBody;


@JacksonXmlRootElement(localName = "soap:Envelope")
public class TransactionStatusRequest extends SOAPEnvelopeRequest<SOAPBody> {
    {
        body = new SOAPBody();
    }

    public static class SOAPBody {
        @JacksonXmlProperty(localName = "checkTransaction")
        public CustomerInfo request = new CustomerInfo();
    }

    public static class CustomerInfo {
        
        
        private String customerNo;
        private String cardData;
        private String msgId;

        public String getMsgid() {
            return msgId;
        }

        public CustomerInfo setMsgid(String msgId) {
            this.msgId = msgId;
            return this;
        }

		public String getCustomerNo() {
			return customerNo;
		}

		public CustomerInfo setCustomerNo(String customerNo) {
			this.customerNo = customerNo;
			return this;
		}

		public String getCardData() {
			return cardData;
		}

		public CustomerInfo setCardData(String cardData) {
			this.cardData = cardData;
			return this;
		}
    }
}