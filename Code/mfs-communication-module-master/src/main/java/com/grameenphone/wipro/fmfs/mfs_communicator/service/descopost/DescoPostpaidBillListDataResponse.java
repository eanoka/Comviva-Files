package com.grameenphone.wipro.fmfs.mfs_communicator.service.descopost;

import java.util.Date;

public class DescoPostpaidBillListDataResponse {

	private String accountNo;
	private String billNo;
	private String billMonth;
	private String billYear;
	private String totalKwh;
	private Double amount;
	private Double lpc;
	private Double vat;
	private Date issueDate;
	private Date dueDate;
	private String paymentStatus;

	public String getAccountNo() {
		return accountNo;
	}

	public void setAccountNo(String accountNo) {
		this.accountNo = accountNo;
	}

	public String getBillNo() {
		return billNo;
	}

	public void setBillNo(String billNo) {
		this.billNo = billNo;
	}

	public String getBillMonth() {
		return billMonth;
	}

	public void setBillMonth(String billMonth) {
		this.billMonth = billMonth;
	}

	public String getBillYear() {
		return billYear;
	}

	public void setBillYear(String billYear) {
		this.billYear = billYear;
	}

	public String getTotalKwh() {
		return totalKwh;
	}

	public void setTotalKwh(String totalKwh) {
		this.totalKwh = totalKwh;
	}

	public Double getAmount() {
		return amount;
	}

	public void setAmount(Double amount) {
		this.amount = amount;
	}

	public Double getLpc() {
		return lpc;
	}

	public void setLpc(Double lpc) {
		this.lpc = lpc;
	}

	public Double getVat() {
		return vat;
	}

	public void setVat(Double vat) {
		this.vat = vat;
	}

	public Date getIssueDate() {
		return issueDate;
	}

	public void setIssueDate(Date issueDate) {
		this.issueDate = issueDate;
	}

	public Date getDueDate() {
		return dueDate;
	}

	public void setDueDate(Date dueDate) {
		this.dueDate = dueDate;
	}

	public String getPaymentStatus() {
		return paymentStatus;
	}

	public void setPaymentStatus(String paymentStatus) {
		this.paymentStatus = paymentStatus;
	}
}
