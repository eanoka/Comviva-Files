package com.grameenphone.wipro.fmfs.mfs_communicator.model.spdb;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.Date;

@JacksonXmlRootElement(localName = "ROOT")
public class AmmeterSoap {
    public static class TopSection {
        @JacksonXmlProperty(localName = "VERSION")
        public String version = "1.0";
        @JacksonXmlProperty(localName = "SOURCE")
        public String source = "0";
        @JacksonXmlProperty(localName = "CUST_ID")
        public String custId;
        @JacksonXmlProperty(localName = "REQUEST_TIME")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone="Asia/Dhaka")
        public Date requestTime = new Date();
    }

    public static class BodySection {
        @JacksonXmlProperty(localName = "RSPCOD")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String responseCode;
        @JacksonXmlProperty(localName = "RSPMSG")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String message;
        @JacksonXmlProperty(localName = "ENEL_ID")
        public String enel;
        @JacksonXmlProperty(localName = "METER_NO")
        public String meter;
        @JacksonXmlProperty(localName = "METERSTATUS")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String status;
        @JacksonXmlProperty(localName = "CUSTOMERNAME")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String customerName;
        @JacksonXmlProperty(localName = "CUSTOMERID")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String customerId;
        @JacksonXmlProperty(localName = "METERID")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String meterId;
        @JacksonXmlProperty(localName = "LASTVENDDATE")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone="Asia/Dhaka")
        public Date lastVendingDate;
        @JacksonXmlProperty(localName = "TEL")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String telephone;
        @JacksonXmlProperty(localName = "ADDRESS")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String address;
        @JacksonXmlProperty(localName = "CUST_TYPE")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String customerType;
        @JacksonXmlProperty(localName = "CCY")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String country;
        @JacksonXmlProperty(localName = "TARIFF")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String tariff;
        @JacksonXmlProperty(localName = "SESSION_ID")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String session;
    }

    public static class TailSection {
        @JacksonXmlProperty(localName = "SIGN_TYPE")
        public String signType = "1";
        @JacksonXmlProperty(localName = "SIGNATURE")
        public String signature;
    }

    @JacksonXmlProperty(localName = "TOP")
    public TopSection topSection = new TopSection();
    @JacksonXmlProperty(localName = "BODY")
    public BodySection bodySection = new BodySection();
    @JacksonXmlProperty(localName = "TAIL")
    public TailSection tailSection = new TailSection();

    @JacksonXmlProperty(localName = "RSPCOD")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String rspcod; //in case of fail
    @JacksonXmlProperty(localName = "RSPMSG")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String message; //in case of fail
}
