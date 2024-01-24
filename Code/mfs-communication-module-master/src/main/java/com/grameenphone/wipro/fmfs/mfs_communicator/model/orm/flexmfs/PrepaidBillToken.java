package com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs;

import javax.persistence.*;

@Entity
@Table(name = "prepaid_bill_token")
public class PrepaidBillToken {
    private int id;
    private String tokenNo;
    private String seqNo;
    private String meterNo;
    private String companyCode;
    private String vendAmnt;
    private String engAmnt;
    private String totalCost;
    private String fees;
    private String penalty;
    private long billPayTableId;

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(name = "token_no", nullable = true)
    public String getTokenNo() {
        return tokenNo;
    }

    public void setTokenNo(String tokenNo) {
        this.tokenNo = tokenNo;
    }

    @Basic
    @Column(name = "seq_no", nullable = true)
    public String getSeqNo() {
        return seqNo;
    }

    public void setSeqNo(String seqNo) {
        this.seqNo = seqNo;
    }

    @Basic
    @Column(name = "meter_no", nullable = true)
    public String getMeterNo() {
        return meterNo;
    }

    public void setMeterNo(String meterNo) {
        this.meterNo = meterNo;
    }

    @Basic
    @Column(name = "company_code", nullable = true)
    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    @Basic
    @Column(name = "vend_amnt", nullable = true)
    public String getVendAmnt() {
        return vendAmnt;
    }

    public void setVendAmnt(String vendAmnt) {
        this.vendAmnt = vendAmnt;
    }

    @Basic
    @Column(name = "eng_amnt", nullable = true)
    public String getEngAmnt() {
        return engAmnt;
    }

    public void setEngAmnt(String engAmnt) {
        this.engAmnt = engAmnt;
    }

    @Basic
    @Column(name = "total_cost", nullable = true)
    public String getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(String totalCost) {
        this.totalCost = totalCost;
    }

    @Basic
    @Column(name = "fees", nullable = true)
    public String getFees() {
        return fees;
    }

    public void setFees(String fees) {
        this.fees = fees;
    }

    @Basic
    @Column(name = "penalty", nullable = true)
    public String getPenalty() {
        return penalty;
    }

    public void setPenalty(String penalty) {
        this.penalty = penalty;
    }

    @Basic
    @Column(name = "bill_pay_table_id", nullable = true)
    public long getBillPayTableId() {
        return billPayTableId;
    }

    public void setBillPayTableId(long billPayTableId) {
        this.billPayTableId = billPayTableId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PrepaidBillToken that = (PrepaidBillToken) o;

        return id == that.id;
    }

    @Override
    public int hashCode() {
        return id == 0 ? super.hashCode() : id;
    }
}