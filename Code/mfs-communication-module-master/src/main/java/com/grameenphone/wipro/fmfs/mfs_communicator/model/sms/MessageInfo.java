package com.grameenphone.wipro.fmfs.mfs_communicator.model.sms;

import java.util.List;
 
public class MessageInfo {
	
	private String msgTransactionId;
	private String validity;
	private String senderId;
	private String deliveryReport;
	private List<Message> message;
	
	

	public String getMsgTransactionId() {
		return msgTransactionId;
	}

	public void setMsgTransactionId(String msgTransactionId) {
		this.msgTransactionId = msgTransactionId;
	}

	public String getValidity() {
		return validity;
	}

	public void setValidity(String validity) {
		this.validity = validity;
	}

	public String getSenderId() {
		return senderId;
	}

	public void setSenderId(String senderId) {
		this.senderId = senderId;
	}

	public String getDeliveryReport() {
		return deliveryReport;
	}

	public void setDeliveryReport(String deliveryReport) {
		this.deliveryReport = deliveryReport;
	}

	public List<Message> getMessage() {
		return message;
	}

	public void setMessage(List<Message> message) {
		this.message = message;
	}
	 
	 
}
