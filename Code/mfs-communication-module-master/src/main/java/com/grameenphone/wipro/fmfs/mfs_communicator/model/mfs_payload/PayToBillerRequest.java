package com.grameenphone.wipro.fmfs.mfs_communicator.model.mfs_payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class PayToBillerRequest {
    @JacksonXmlProperty(localName = "AMOUNT")
    public Number amount;
    @JacksonXmlProperty(localName = "CATEGORY")
    public String category;
    @JacksonXmlProperty(localName = "SURCHARGE")
    @JsonInclude(Include.NON_NULL)
    public Integer surcharge = null;
    @JacksonXmlProperty(localName = "SESSION_ID")
    public String sessionId;
    @JacksonXmlProperty(localName = "PREF2")
    public String pref2;
    @JacksonXmlProperty(localName = "BILLNO")
    public String billNo;
    @JacksonXmlProperty(localName = "PIN")
    public String pin;
    @JacksonXmlProperty(localName = "BILLCCODE")
    public String companyCode;
    @JacksonXmlProperty(localName = "BILLANO")
    public String account;
    @JacksonXmlProperty(localName = "MSISDN")
    public String payerMsisdn;
    @JacksonXmlProperty(localName = "TYPE")
    public String type;
    @JacksonXmlProperty(localName = "PREF3")
    public String pref3;
    @JacksonXmlProperty(localName = "STATIC_MENU")
    public String menu = "C";
    @JacksonXmlProperty(localName = "PAYMENT_INSTRUMENT")
    public String instrument = "WALLET";
    @JacksonXmlProperty(localName = "BPROVIDER")
    public String bProvider = "101";
    @JacksonXmlProperty(localName = "PROVIDER")
    public String provider = "101";
    @JacksonXmlProperty(localName = "PAYID")
    public String payId = "12";
}