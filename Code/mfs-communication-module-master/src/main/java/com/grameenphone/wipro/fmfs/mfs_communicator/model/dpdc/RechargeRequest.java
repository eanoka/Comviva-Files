package com.grameenphone.wipro.fmfs.mfs_communicator.model.dpdc;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.dpdc.RechargeRequest.SOAPBody;

@JacksonXmlRootElement(localName = "soap:Envelope")
public class RechargeRequest extends SOAPEnvelopeRequest<SOAPBody> {
    {
        body = new SOAPBody();
    }

    public static class SOAPBody {
        @JacksonXmlProperty(localName = "createRecharge")
        public RechargeKeypadMeterWMsg request = new RechargeKeypadMeterWMsg();
    }

    public static class RechargeKeypadMeterWMsg {
        private String customerNo;
        private String amount;
        private String cardData;
        private String msgId;       
     

        public String getAmount() {
            return amount;
        }

        public RechargeKeypadMeterWMsg setAmount(String amount) {
            this.amount = amount;
            return this;
        }

        public String getMsgId() {
            return msgId;
        }

        public void setMsgId(String msgId) {
            this.msgId = msgId;
        }

		public String getCustomerNo() {
			return customerNo;
		}

		public RechargeKeypadMeterWMsg setCustomerNo(String customerNo) {
			this.customerNo = customerNo;
			return this;
		}

		public String getCardData() {
			return cardData;
		}

		public RechargeKeypadMeterWMsg setCardData(String cardData) {
			this.cardData = cardData;
			return this;
		}
    }
}