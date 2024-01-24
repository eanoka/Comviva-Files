package com.grameenphone.wipro.fmfs.mfs_communicator.model.dpdc;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

@JacksonXmlRootElement(localName = "Vending")
public class DpdcVendingDetail {
    @JacksonXmlProperty(localName = "orderid" )
    private String orderId;

    @JacksonXmlProperty(localName = "orderdatetime")
    private String orderDateTime;

    @JacksonXmlProperty(localName = "meterno")
    private String meterNo;

    @JacksonXmlProperty(localName = "customerno")
    private String customerNo;

    @JacksonXmlProperty(localName = "customername")
    private String customerName;

    @JacksonXmlProperty(localName = "operatorname")
    private String operatorName;

    @JacksonXmlProperty(localName = "posid")
    private String posId;

    @JacksonXmlProperty(localName = "posbalance")
    private String posBalance;

    @JacksonXmlProperty(localName = "energycost")
    private String energyCost;

    @JacksonXmlProperty(localName = "paidamount")
    private String paidAmount;

    @JacksonXmlProperty(localName = "grossamount")
    private String grossAmount;

    private String penalty;

    @JacksonXmlProperty(localName = "Token")
    private String token;

    @JacksonXmlProperty(localName = "sequences")
    private String sequence;

    private String tariff;

    private String office;

    @JacksonXmlProperty(localName = "TotalFee")
    private String totalFee;

    @JacksonXmlProperty(localName = "Fees")
    private Fees fees;

    @JacksonXmlProperty(localName = "MsgID")
    private String msgId;

    @JacksonXmlProperty(localName = "paydebt")
    private String paydebt;
    
    @JacksonXmlProperty(localName = "sanctionload")
    private String sanctionload;
    
    @JacksonXmlProperty(localName = "onlineRecharge")
    private String onlineRecharge;
    
    @JacksonXmlProperty(localName = "RechargeStatus")
    private String rechargeStatus;

    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getOrderDateTime() {
        return orderDateTime;
    }

    public void setOrderDateTime(String orderDateTime) {
        this.orderDateTime = orderDateTime;
    }

    public String getMeterNo() {
        return meterNo;
    }

    public void setMeterNo(String meterNo) {
        this.meterNo = meterNo;
    }

    public String getCustomerNo() {
        return customerNo;
    }

    public void setCustomerNo(String customerNo) {
        this.customerNo = customerNo;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getPosId() {
        return posId;
    }

    public void setPosId(String posId) {
        this.posId = posId;
    }

    public String getPosBalance() {
        return posBalance;
    }

    public void setPosBalance(String posBalance) {
        this.posBalance = posBalance;
    }

    public String getEnergyCost() {
        return energyCost;
    }

    public void setEnergyCost(String energyCost) {
        this.energyCost = energyCost;
    }

    public String getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(String paidAmount) {
        this.paidAmount = paidAmount;
    }

    public String getGrossAmount() {
        return grossAmount;
    }

    public void setGrossAmount(String grossAmount) {
        this.grossAmount = grossAmount;
    }

    public String getPenalty() {
        return penalty;
    }

    public void setPenalty(String penalty) {
        this.penalty = penalty;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public String getTariff() {
        return tariff;
    }

    public void setTariff(String tariff) {
        this.tariff = tariff;
    }

    public String getOffice() {
        return office;
    }

    public void setOffice(String office) {
        this.office = office;
    }

    public String getTotalFee() {
        return totalFee;
    }

    public void setTotalFee(String totalFee) {
        this.totalFee = totalFee;
    }

    public Fees getFees() {
        return fees;
    }

    public void setFees(Fees fees) {
        this.fees = fees;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public static class Fees {
        @JacksonXmlProperty(localName = "Fee")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<DpdcVendingDetail.Fee> fee;

        public List<DpdcVendingDetail.Fee> getFee() {
            return fee;
        }

        public void setFee(List<DpdcVendingDetail.Fee> fee) {
            this.fee = fee;
        }
    }

    public static class Fee {
        @JacksonXmlProperty(localName = "FeeName")
        private String feeName;

        @JacksonXmlProperty(localName = "FeeValue")
        private String feeValue;

        public String getFeeName() {
            return feeName;
        }

        public void setFeeName(String feeName) {
            this.feeName = feeName;
        }

        public String getFeeValue() {
            return feeValue;
        }

        public void setFeeValue(String feeValue) {
            this.feeValue = feeValue;
        }
    }

    public String getPaydebt() {
        return paydebt;
    }

    public void setPaydebt(String paydebt) {
        this.paydebt = paydebt;
    }

	public String getSanctionload() {
		return sanctionload;
	}

	public void setSanctionload(String sanctionload) {
		this.sanctionload = sanctionload;
	}

	public String getOnlineRecharge() {
		return onlineRecharge;
	}

	public void setOnlineRecharge(String onlineRecharge) {
		this.onlineRecharge = onlineRecharge;
	}

	public String getRechargeStatus() {
		return rechargeStatus;
	}

	public void setRechargeStatus(String rechargeStatus) {
		this.rechargeStatus = rechargeStatus;
	}
}