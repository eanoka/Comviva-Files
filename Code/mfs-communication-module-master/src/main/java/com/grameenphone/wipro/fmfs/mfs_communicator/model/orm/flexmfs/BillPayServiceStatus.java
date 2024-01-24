package com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "bill_pay_service_status")
public class BillPayServiceStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String sessionId;
    @Column(name = "order_id")
    private String orderID;
    private String msisdn;
    private String categoryCode;
    private String companyCode;
    private String accountNo;
    private String billNo;
    private double amount;
    private String mfsTxnid;
    private String thirdPartyTxnid;
    private String mfsTxnStatus;
    private String thirdPartyTxnStatus;
    private Long serviceExecutionTime;
    private String status;
    private String createdBy; // channel specific creator information (optional)
    private Timestamp creationDate;
    private String lastUpdatedBy;  // is of no use
    @Column(name = "attr_1")
    private String attr1; // (company specific)
    @Column(name = "attr_2")
    private String attr2; // (company specific) for prepaid being user for storing token - however now token has been ported to new table
    @Column(name = "attr_3")
    private String attr3; // (company specific)
    @Column(name = "attr_4")
    private String attr4; // being used to store reversal transaction id
    private String customerMsisdn;
    private String transactionType;
    private String channel;
    private Double serviceCharge;
    private Double paidAmount;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

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

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getMfsTxnid() {
        return mfsTxnid;
    }

    public void setMfsTxnid(String mfsTxnid) {
        this.mfsTxnid = mfsTxnid;
    }

    public String getThirdPartyTxnid() {
        return thirdPartyTxnid;
    }

    public void setThirdPartyTxnid(String thirdPartyTxnid) {
        this.thirdPartyTxnid = thirdPartyTxnid;
    }

    public String getMfsTxnStatus() {
        return mfsTxnStatus;
    }

    public void setMfsTxnStatus(String mfsTxnStatus) {
        this.mfsTxnStatus = mfsTxnStatus;
    }

    public String getThirdPartyTxnStatus() {
        return thirdPartyTxnStatus;
    }

    public void setThirdPartyTxnStatus(String thirdPartyTxnStatus) {
        this.thirdPartyTxnStatus = thirdPartyTxnStatus;
    }

    public Long getServiceExecutionTime() {
        return serviceExecutionTime;
    }

    public void setServiceExecutionTime(Long serviceExecutionTime) {
        this.serviceExecutionTime = serviceExecutionTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Timestamp getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Timestamp creationDate) {
        this.creationDate = creationDate;
    }

    public String getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public void setLastUpdatedBy(String lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
    }

    public String getAttr1() {
        return attr1;
    }

    public void setAttr1(String attr1) {
        this.attr1 = attr1;
    }

    public String getAttr2() {
        return attr2;
    }

    public void setAttr2(String attr2) {
        this.attr2 = attr2;
    }

    public String getAttr3() {
        return attr3;
    }

    public void setAttr3(String attr3) {
        this.attr3 = attr3;
    }

    public String getAttr4() {
        return attr4;
    }

    public void setAttr4(String attr4) {
        this.attr4 = attr4;
    }

    public String getCustomerMsisdn() {
        return customerMsisdn;
    }

    public void setCustomerMsisdn(String customerMsisdn) {
        this.customerMsisdn = customerMsisdn;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Double getServiceCharge() {
        return serviceCharge;
    }

    public void setServiceCharge(Double serviceCharge) {
        this.serviceCharge = serviceCharge;
    }

    public Double getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(Double paidAmount) {
        this.paidAmount = paidAmount;
    }

    public String getOrderID() {
        return orderID;
    }

    public void setOrderID(String orderID) {
        this.orderID = orderID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BillPayServiceStatus that = (BillPayServiceStatus) o;

        return id == that.id;
    }

    @Override
    public int hashCode() {
        return id == 0 ? super.hashCode() : (int)id;
    }
}