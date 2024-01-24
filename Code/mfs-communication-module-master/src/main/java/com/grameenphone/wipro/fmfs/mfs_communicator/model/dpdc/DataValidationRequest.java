package com.grameenphone.wipro.fmfs.mfs_communicator.model.dpdc;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.dpdc.AmountValidationRequest.SOAPBody;

@JacksonXmlRootElement(localName = "soap:Envelope") 
public class DataValidationRequest extends SOAPEnvelopeDataRequest<SOAPBody> {

	@JacksonXmlProperty(localName = "soap:Body")
	public SOAPBody body = new SOAPBody();

	public static class SOAPBody {

		@JacksonXmlProperty(localName = "dataValidation")
		public DataValidation dataValidation = new DataValidation(); 

	}

	public static class DataValidation {

		private String customerNo;
		private String amount;
		private String cardData;

		public String getCustomerNo() {
			return customerNo;
		}

		public void setCustomerNo(String customerNo) {
			this.customerNo = customerNo;
		}

		public String getAmount() {
			return amount;
		}

		public void setAmount(String amount) {
			this.amount = amount;
		}

		public String getCardData() {
			return cardData;
		}

		public void setCardData(String cardData) {
			this.cardData = cardData;
		}

	}

}
