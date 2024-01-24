package com.grameenphone.wipro.fmfs.mfs_communicator.model.mfs_payload;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Created by shain.shahid on 5/17/2018.
 */
@JacksonXmlRootElement(localName = "COMMAND")
public class MobiquityReversalSecondRequest {
    private static final String TYPE = "REQTRCORCF";
    private static final String ACTION = "TS";
    private static final String BLOCK_SMS = "PAYER";

    {
        setType(TYPE);
        setAction(ACTION);
        setBlockSms(BLOCK_SMS);
    }

    @JacksonXmlProperty(localName = "TYPE")
    private String type;

    @JacksonXmlProperty(localName = "USERID")
    private String userId;

    @JacksonXmlProperty(localName = "ACTION")
    private String action;

    @JacksonXmlProperty(localName = "TXNID")
    private String txnId;

    @JacksonXmlProperty(localName = "BLOCKSMS")
    private String blockSms;

    @JacksonXmlProperty(localName = "SYSTEMTYPE")
    private String systemType;

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
}
