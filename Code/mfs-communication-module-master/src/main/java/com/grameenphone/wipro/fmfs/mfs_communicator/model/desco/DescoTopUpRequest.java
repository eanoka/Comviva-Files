package com.grameenphone.wipro.fmfs.mfs_communicator.model.desco;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DescoTopUpRequest {
    private static final String DATE_FORMAT="MM/dd/yyyy HH:mm:ss";
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT);

    private String rechargeId;
    private String transactionId;
    private String contactNo;
    private String agentId;
    private String apiKey;
    private String rechargeAt;

    public DescoTopUpRequest() {
        this.rechargeAt = simpleDateFormat.format(new Date());
    }

    public String getRechargeId() {
        return rechargeId;
    }

    public void setRechargeId(String rechargeId) {
        this.rechargeId = rechargeId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getContactNo() {
        return contactNo;
    }

    public void setContactNo(String contactNo) {
        this.contactNo = contactNo;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getRechargeAt() {
        return rechargeAt;
    }

    public void setRechargeAt(Date rechargeAt) {
        this.rechargeAt = simpleDateFormat.format(rechargeAt);
    }

}
