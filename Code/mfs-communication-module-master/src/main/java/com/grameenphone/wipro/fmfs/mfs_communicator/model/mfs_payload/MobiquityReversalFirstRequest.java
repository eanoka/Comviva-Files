package com.grameenphone.wipro.fmfs.mfs_communicator.model.mfs_payload;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Created by shain.shahid on 5/17/2018.
 */
@JacksonXmlRootElement(localName = "COMMAND")
public class MobiquityReversalFirstRequest {
    private static final String RETAILER_USER_TYPE = "CHANNEL";
    private static final String CUSTOMER_USER_TYPE = "SUBSCRIBER";
    private static final String TYPE = "REQTRCORIN";
    private static final String USERTYPE2 = "WBILLMER";
    private static final String SCREVERSAL = "Y";
    private static final String ACTION = "TI";

    public MobiquityReversalFirstRequest() {
        setType(TYPE);
        setUserType2(USERTYPE2);
        setScReversal(SCREVERSAL);
        setAction(ACTION);
    }

    @JacksonXmlProperty(localName = "TYPE")
    private String type;

    @JacksonXmlProperty(localName = "USERID")
    private String userId;

    @JacksonXmlProperty(localName = "USERTYPE")
    private String userType;

    @JacksonXmlProperty(localName ="USERTYPE2")
    private String userType2;

    @JacksonXmlProperty(localName = "SCREVERSAL")
    private String scReversal;

    @JacksonXmlProperty(localName = "ACTION")
    private String action;

    @JacksonXmlProperty(localName = "TXNID")
    private String txnId;

    @JacksonXmlProperty(localName = "REMARKS")
    private String remarks;

    @JacksonXmlProperty(localName="BLOCKSMS")
    private String blockSms;

    @JacksonXmlProperty(localName = "SYSTEMTYPE")
    private String systemType;

    @JsonIgnore
    private boolean isRetailer;
    
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getScReversal() {
        return scReversal;
    }

    public void setScReversal(String scReversal) {
        this.scReversal = scReversal;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTxnId() {
        return txnId;
    }

    public void setTxnId(String txnId) {
        this.txnId = txnId;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getBlockSms() {
        return blockSms;
    }

    public void setBlockSms(String blockSms) {
        this.blockSms = blockSms;
    }

    public String getSystemType() {
        return systemType;
    }

    public void setSystemType(String systemType) {
        this.systemType = systemType;
    }

    public String getUserType2() {
        return userType2;
    }

    public void setUserType2(String userType2) {
        this.userType2 = userType2;
    }

    public boolean isRetailer() {
        return isRetailer;
    }

    public void setRetailer(boolean retailer) {
        isRetailer = retailer;

        if(isRetailer) {
            setUserType(RETAILER_USER_TYPE);
        } else {
            setUserType(CUSTOMER_USER_TYPE);
        }
    }
}