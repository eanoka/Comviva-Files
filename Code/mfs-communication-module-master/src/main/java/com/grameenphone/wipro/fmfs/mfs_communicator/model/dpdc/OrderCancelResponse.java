package com.grameenphone.wipro.fmfs.mfs_communicator.model.dpdc;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "OrderCancel")
public class OrderCancelResponse {
	 @JacksonXmlProperty(localName = "posid" )
	    private String posId;

	    @JacksonXmlProperty(localName = "orderid")
	    private String orderId;

	    @JacksonXmlProperty(localName = "meterno")
	    private String meterNo;

	    @JacksonXmlProperty(localName = "MsgID")
	    private String msgId;

	    @JacksonXmlProperty(localName = "RechargeStatus")
	    private String rechargeStatus;

	    @JacksonXmlProperty(localName = "Details")
	    private String details;

		public String getPosId() {
			return posId;
		}

		public void setPosId(String posId) {
			this.posId = posId;
		}

		public String getOrderId() {
			return orderId;
		}

		public void setOrderId(String orderId) {
			this.orderId = orderId;
		}

		public String getMeterNo() {
			return meterNo;
		}

		public void setMeterNo(String meterNo) {
			this.meterNo = meterNo;
		}

		public String getMsgId() {
			return msgId;
		}

		public void setMsgId(String msgId) {
			this.msgId = msgId;
		}

		public String getRechargeStatus() {
			return rechargeStatus;
		}

		public void setRechargeStatus(String rechargeStatus) {
			this.rechargeStatus = rechargeStatus;
		}

		public String getDetails() {
			return details;
		}

		public void setDetails(String details) {
			this.details = details;
		}

}
