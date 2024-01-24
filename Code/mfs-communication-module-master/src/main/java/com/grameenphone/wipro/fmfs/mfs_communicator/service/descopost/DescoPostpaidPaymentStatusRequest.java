package com.grameenphone.wipro.fmfs.mfs_communicator.service.descopost;

public class DescoPostpaidPaymentStatusRequest {
	
	public String billNumber;
	public String transactionId;
	
	public String getBillNumber() {
		return billNumber;
	}
	public void setBillNumber(String billNumber) {
		this.billNumber = billNumber;
	}
	public String getTransactionId() {
		return transactionId;
	}
	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}
}
