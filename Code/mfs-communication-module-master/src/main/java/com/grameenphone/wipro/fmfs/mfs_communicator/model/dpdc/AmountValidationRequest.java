package com.grameenphone.wipro.fmfs.mfs_communicator.model.dpdc;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.dpdc.AmountValidationRequest.SOAPBody;

@JacksonXmlRootElement(localName = "soap:Envelope")
public class AmountValidationRequest extends SOAPEnvelopeRequest<SOAPBody> {
    {
        body = new SOAPBody();
    }

    public static class SOAPBody {
        @JacksonXmlProperty(localName = "dataValidation")
        public CustomerInfoWithAmount request = new CustomerInfoWithAmount();
    }

    public static class CustomerInfoWithAmount {
        
        
        private String customerNo;
        private String amount;
        private String cardData;

        public String getAmount() {
            return amount;
        }

        public CustomerInfoWithAmount setAmount(String amount) {
            this.amount = amount;
            return this;
        }

		public String getCustomerNo() {
			return customerNo;
		}

		public CustomerInfoWithAmount setCustomerNo(String customerNo) {
			this.customerNo = customerNo;
			return this;
		}

		public String getCardData() {
			return cardData;
		}

		public CustomerInfoWithAmount setCardData(String cardData) {
			this.cardData = cardData;
			return this;
		}
    }
}