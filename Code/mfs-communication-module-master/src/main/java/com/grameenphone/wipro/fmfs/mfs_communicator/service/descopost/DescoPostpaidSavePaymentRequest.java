package com.grameenphone.wipro.fmfs.mfs_communicator.service.descopost;

public class DescoPostpaidSavePaymentRequest {
	public String billNumber;
	public String billToken;
	public String transactionId;
	public String bankCode;
	public String scrollNo;
	public double totalPayableAmount;
	public double totalPaidAmount;
	public double lpc;
	public int paid;
	public String issueDate;
	public String dueDate;
	public String cTariff;
	public String departmentCode;
	public double paymentAmount;
	public double paymentVatAmount;
	public String paymentDate;
	public String trackingNo;
	public double stampQty;
	public String transactionTrackingNo;
	public String tranAccount;
	public String transactionDateTime;

	public String getBillNumber() {
		return billNumber;
	}

	public void setBillNumber(String billNumber) {
		this.billNumber = billNumber;
	}

	public String getBillToken() {
		return billToken;
	}

	public void setBillToken(String billToken) {
		this.billToken = billToken;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	public String getBankCode() {
		return bankCode;
	}

	public void setBankCode(String bankCode) {
		this.bankCode = bankCode;
	}

	public String getScrollNo() {
		return scrollNo;
	}

	public void setScrollNo(String scrollNo) {
		this.scrollNo = scrollNo;
	}

	public double getTotalPayableAmount() {
		return totalPayableAmount;
	}

	public void setTotalPayableAmount(double totalPayableAmount) {
		this.totalPayableAmount = totalPayableAmount;
	}

	public double getTotalPaidAmount() {
		return totalPaidAmount;
	}

	public void setTotalPaidAmount(double totalPaidAmount) {
		this.totalPaidAmount = totalPaidAmount;
	}

	public double getLpc() {
		return lpc;
	}

	public void setLpc(double lpc) {
		this.lpc = lpc;
	}

	public int getPaid() {
		return paid;
	}

	public void setPaid(int paid) {
		this.paid = paid;
	}

	public String getIssueDate() {
		return issueDate;
	}

	public void setIssueDate(String issueDate) {
		this.issueDate = issueDate;
	}

	public String getDueDate() {
		return dueDate;
	}

	public void setDueDate(String dueDate) {
		this.dueDate = dueDate;
	}

	public String getcTariff() {
		return cTariff;
	}

	public void setcTariff(String cTariff) {
		this.cTariff = cTariff;
	}

	public String getDepartmentCode() {
		return departmentCode;
	}

	public void setDepartmentCode(String departmentCode) {
		this.departmentCode = departmentCode;
	}

	public double getPaymentAmount() {
		return paymentAmount;
	}

	public void setPaymentAmount(double paymentAmount) {
		this.paymentAmount = paymentAmount;
	}

	public double getPaymentVatAmount() {
		return paymentVatAmount;
	}

	public void setPaymentVatAmount(double paymentVatAmount) {
		this.paymentVatAmount = paymentVatAmount;
	}

	public String getPaymentDate() {
		return paymentDate;
	}

	public void setPaymentDate(String paymentDate) {
		this.paymentDate = paymentDate;
	}

	public String getTrackingNo() {
		return trackingNo;
	}

	public void setTrackingNo(String trackingNo) {
		this.trackingNo = trackingNo;
	}

	public double getStampQty() {
		return stampQty;
	}

	public void setStampQty(double stampQty) {
		this.stampQty = stampQty;
	}

	public String getTransactionTrackingNo() {
		return transactionTrackingNo;
	}

	public void setTransactionTrackingNo(String transactionTrackingNo) {
		this.transactionTrackingNo = transactionTrackingNo;
	}

	public String getTranAccount() {
		return tranAccount;
	}

	public void setTranAccount(String tranAccount) {
		this.tranAccount = tranAccount;
	}

	public String getTransactionDateTime() {
		return transactionDateTime;
	}

	public void setTransactionDateTime(String transactionDateTime) {
		this.transactionDateTime = transactionDateTime;
	}
   
}