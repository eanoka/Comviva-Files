package com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "dispute_transactions")
public class DisputeTransaction {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "meter_no")
    private String meterNo;

    @Column(name = "bill_no")
    private String billNo;

    @Column(name = "amount")
    private double amount;

    @Column(name = "initiator")
    private String initiator;

    @Column(name = "customer_msisdn")
    private String customerMsisdn;

    @Column(name = "status")
    private String status;

    @Column(name = "attr_1")
    private String attr_1;

    @Column(name = "attr_2")
    private String attr_2;
    @Column(name = "attr_3")
    private String attr_3;
    @Column(name = "attr_4")
    private String attr_4;

    @Column(name = "creation_date")
    private Timestamp creationDate;
    
    @Column(name = "last_update_date")
    private Timestamp lastUpdateDate;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "bill_pay_status_table_id")
    private BillPayServiceStatus billPayServiceStatus;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMeterNo() {
        return meterNo;
    }

    public void setMeterNo(String meterNo) {
        this.meterNo = meterNo;
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

    public String getInitiator() {
        return initiator;
    }

    public void setInitiator(String initiator) {
        this.initiator = initiator;
    }

    public String getCustomerMsisdn() {
        return customerMsisdn;
    }

    public void setCustomerMsisdn(String customerMsisdn) {
        this.customerMsisdn = customerMsisdn;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAttr_1() {
        return attr_1;
    }

    public void setAttr_1(String attr_1) {
        this.attr_1 = attr_1;
    }

    public String getAttr_2() {
        return attr_2;
    }

    public void setAttr_2(String attr_2) {
        this.attr_2 = attr_2;
    }

    public String getAttr_3() {
        return attr_3;
    }

    public void setAttr_3(String attr_3) {
        this.attr_3 = attr_3;
    }

    public String getAttr_4() {
        return attr_4;
    }

    public void setAttr_4(String attr_4) {
        this.attr_4 = attr_4;
    }

    public Timestamp getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Timestamp creationDate) {
        this.creationDate = creationDate;
    }

    public BillPayServiceStatus getBillPayServiceStatus() {
        return billPayServiceStatus;
    }

    public void setBillPayServiceStatus(BillPayServiceStatus billPayServiceStatus) {
        this.billPayServiceStatus = billPayServiceStatus;
    }

	public Timestamp getLastUpdateDate() {
		return lastUpdateDate;
	}

	public void setLastUpdateDate(Timestamp lastUpdateDate) {
		this.lastUpdateDate = lastUpdateDate;
	}
    
}