package com.grameenphone.wipro.fmfs.mfs_communicator.model.mfs_payload;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class AssociationRequest {
    @JacksonXmlProperty(localName = "CATEGORY")
    public String category;
    @JacksonXmlProperty(localName = "PREF1")
    public String consumerId;
    @JacksonXmlProperty(localName = "SESSION_ID")
    public String sessionId;
    @JacksonXmlProperty(localName = "STATIC_MENU")
    public String staticMenu = "C";
    @JacksonXmlProperty(localName = "BILLCCODE")
    public String companyCode;
    @JacksonXmlProperty(localName = "PROVIDER")
    public Integer provider = 101;
    @JacksonXmlProperty(localName = "MSISDN")
    public String msisdn;
    @JacksonXmlProperty(localName = "TYPE")
    public String type;
}