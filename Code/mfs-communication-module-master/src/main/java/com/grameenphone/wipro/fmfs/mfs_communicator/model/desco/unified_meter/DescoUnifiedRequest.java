package com.grameenphone.wipro.fmfs.mfs_communicator.model.desco.unified_meter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "xml")
public class DescoUnifiedRequest {
    //Used for All API
    @JacksonXmlProperty(isAttribute = true)
    public String userName;

    @JacksonXmlProperty(isAttribute = true)
    public String userPass;

    @JacksonXmlProperty(isAttribute = true)
    public String apiKey;

    @JacksonXmlProperty(isAttribute = true)
    public String accountNo;

    //Used for Amount validation API and Payment API
    @JacksonXmlProperty(isAttribute = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String transId;

    @JacksonXmlProperty(isAttribute = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String amount;

    @JacksonXmlProperty(isAttribute = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String meterNo;

    //Used for Amount validation API
    @JacksonXmlProperty(localName = "Phone", isAttribute = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String phone;

    //Used for Payment API
    @JacksonXmlProperty(isAttribute = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String calcMode;

    @JacksonXmlProperty(isAttribute = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String mobile;

    //Used for Acknowledgement API
    @JacksonXmlProperty(isAttribute = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String refCode;

    @JacksonXmlProperty(isAttribute = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String vendingMode;
}