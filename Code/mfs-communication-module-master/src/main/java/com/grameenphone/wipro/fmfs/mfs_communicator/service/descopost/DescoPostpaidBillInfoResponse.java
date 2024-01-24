package com.grameenphone.wipro.fmfs.mfs_communicator.service.descopost;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class DescoPostpaidBillInfoResponse extends DescoPostpaidBaseResponse{
	
	public String billNo;
    public String billToken;
    public String accountNo;
    public String meterNo;
    public String year;
    public String month;
    public double totalAmount;
    public double totalVat;
    public String issueDate;
    public String departmentCode;
    public String dueDate;
    public String lpc;
    public String tariff;
    public String consumerName;
    public String address;
    public String paymentType;
    public String paymentStatus;
    public double totalAmountTobePaid;
    public Object organizationCode;
    public String totalKwh;
    
	public String getBillNo() {
		return billNo;
	}
	public void setBillNo(String billNo) {
		this.billNo = billNo;
	}
	public String getBillToken() {
		return billToken;
	}
	public void setBillToken(String billToken) {
		this.billToken = billToken;
	}
	public String getAccountNo() {
		return accountNo;
	}
	public void setAccountNo(String accountNo) {
		this.accountNo = accountNo;
	}
	public String getMeterNo() {
		return meterNo;
	}
	public void setMeterNo(String meterNo) {
		this.meterNo = meterNo;
	}
	public String getYear() {
		return year;
	}
	public void setYear(String year) {
		this.year = year;
	}
	public String getMonth() {
		return month;
	}
	public void setMonth(String month) {
		this.month = month;
	}
	public double getTotalAmount() {
		return totalAmount;
	}
	public void setTotalAmount(double totalAmount) {
		this.totalAmount = totalAmount;
	}
	public double getTotalVat() {
		return totalVat;
	}
	public void setTotalVat(double totalVat) {
		this.totalVat = totalVat;
	}
	public String getIssueDate() {
		return issueDate;
	}
	public void setIssueDate(String issueDate) {
		this.issueDate = issueDate;
	}
	public String getDepartmentCode() {
		return departmentCode;
	}
	public void setDepartmentCode(String departmentCode) {
		this.departmentCode = departmentCode;
	}
	public String getDueDate() {
		return dueDate;
	}
	public void setDueDate(String dueDate) {
		this.dueDate = dueDate;
	}
	public String getLpc() {
		return lpc;
	}
	public void setLpc(String lpc) {
		this.lpc = lpc;
	}
	public String getTariff() {
		return tariff;
	}
	public void setTariff(String tariff) {
		this.tariff = tariff;
	}
	public String getConsumerName() {
		return consumerName;
	}
	public void setConsumerName(String consumerName) {
		this.consumerName = consumerName;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getPaymentType() {
		return paymentType;
	}
	public void setPaymentType(String paymentType) {
		this.paymentType = paymentType;
	}
	public String getPaymentStatus() {
		return paymentStatus;
	}
	public void setPaymentStatus(String paymentStatus) {
		this.paymentStatus = paymentStatus;
	}
	public double getTotalAmountTobePaid() {
		return totalAmountTobePaid;
	}
	public void setTotalAmountTobePaid(double totalAmountTobePaid) {
		this.totalAmountTobePaid = totalAmountTobePaid;
	}
	public Object getOrganizationCode() {
		return organizationCode;
	}
	public void setOrganizationCode(Object organizationCode) {
		this.organizationCode = organizationCode;
	}
	public String getTotalKwh() {
		return totalKwh;
	}
	public void setTotalKwh(String totalKwh) {
		this.totalKwh = totalKwh;
	}
}