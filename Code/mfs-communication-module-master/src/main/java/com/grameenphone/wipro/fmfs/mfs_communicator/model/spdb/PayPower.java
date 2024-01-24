package com.grameenphone.wipro.fmfs.mfs_communicator.model.spdb;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.grameenphone.wipro.utility.marshal.JacksonCommaIncludedNumberDeserializer;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.spdb.AmmeterSoap.TopSection;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.spdb.AmmeterSoap.TailSection;

import java.util.Date;
import java.util.List;

@JacksonXmlRootElement(localName = "ROOT")
public class PayPower {
    public static class BodySection {
        //region REQUEST PART
        @JacksonXmlProperty(localName = "USER_ID")
        public String user;
        @JacksonXmlProperty(localName = "POWER_AMT")
        public String powerAmount;
        @JacksonXmlProperty(localName = "ENEL_ID")
        public String enel;
        @JacksonXmlProperty(localName = "METER_NO")
        public String meter;
        @JacksonXmlProperty(localName = "SESSION_ID")
        public String session;
        //endregion

        //region RESPONSE PART
        @JacksonXmlProperty(localName = "RSPCOD")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String responseCode;
        @JacksonXmlProperty(localName = "RSPMSG")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String message;
        @JacksonXmlProperty(localName = "ACCOUNT")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String account;
        @JacksonXmlProperty(localName = "ORDERTIME")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone="Asia/Dhaka")
        public Date orderTime;
        @JacksonXmlProperty(localName = "PRDORDNO")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String trxNo;
        @JacksonXmlProperty(localName = "RESOURCE_TYPE")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String resourceType;
        @JacksonXmlProperty(localName = "ORDAMT")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonDeserialize(using = JacksonCommaIncludedNumberDeserializer.class)
        public Double amount;
        @JacksonXmlProperty(localName = "FEE")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonDeserialize(using = JacksonCommaIncludedNumberDeserializer.class)
        public Double fee;
        @JacksonXmlProperty(localName = "ENERGYCONST")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonDeserialize(using = JacksonCommaIncludedNumberDeserializer.class)
        public Double energyCost;
        @JacksonXmlProperty(localName = "VAT")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonDeserialize(using = JacksonCommaIncludedNumberDeserializer.class)
        public Double vat;
        @JacksonXmlProperty(localName = "TOKEN")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String token;
        @JacksonXmlProperty(localName = "ENERGY")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonDeserialize(using = JacksonCommaIncludedNumberDeserializer.class)
        public Double energy;
        @JacksonXmlProperty(localName = "ACCOUNTSAVEAMOUNT")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonDeserialize(using = JacksonCommaIncludedNumberDeserializer.class)
        public Double savedAmount;
        @JacksonXmlProperty(localName = "ACCOUNTPAYAMOUNT")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonDeserialize(using = JacksonCommaIncludedNumberDeserializer.class)
        public Double payAmount;
        @JacksonXmlProperty(localName = "CCY")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String currency;
        @JacksonXmlProperty(localName = "CASH_AC_BAL")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonDeserialize(using = JacksonCommaIncludedNumberDeserializer.class)
        public Double acBalance;
        @JacksonXmlElementWrapper(useWrapping = false)
        public List<VAT_DETAIL> VAT_DETAIL;
        //endregion
    }

    @JacksonXmlRootElement(localName="VAT_DETAIL")
    public static class VAT_DETAIL {
        @JacksonXmlProperty(localName="ITEM_NAME")
        public String ITEM_NAME;
        @JacksonXmlProperty(localName="ITEM_VALUE")
        public String ITEM_VALUE;
    }

    @JacksonXmlProperty(localName = "TOP")
    public TopSection topSection = new TopSection();
    @JacksonXmlProperty(localName = "BODY")
    public BodySection bodySection = new BodySection();
    @JacksonXmlProperty(localName = "TAIL")
    public AmmeterSoap.TailSection tailSection = new TailSection();

    @JacksonXmlProperty(localName = "RSPCOD")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String responseCode; //in case of fail
    @JacksonXmlProperty(localName = "RSPMSG")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String message; //in case of fail
}
