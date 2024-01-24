package com.grameenphone.wipro.fmfs.mfs_communicator.model.desco.unified_meter;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.Date;
import java.util.List;

@JacksonXmlRootElement(localName = "data")
public class DescoUnifiedResponse {
    @JacksonXmlProperty(isAttribute = true)
    public Integer state;

    public String meterNo;
    public String meterType;
    public String accountNo;
    public String accountName;
    public String tariffProgram;
    public String dues;
    public String transId;
    public String message;

    //Used for Account Validation APi
    @JacksonXmlProperty(localName = "cellphone")
    public String cellPhone;
    @JacksonXmlProperty(localName = "tokenDataList")
    public TokenDataList tokenDataList;

    public static class TokenDataList{
        public String cardData;
        public String psOldPwd;
        public String psNewPwd;
        @JacksonXmlProperty(localName = "tokenData")
        @JacksonXmlElementWrapper(useWrapping = false)
        public List<TokenData> tokenData;
    }
    public static class TokenData {
        public String ordersId;
        public String orderType;
        public String operator;
        public String totalAmount;
        public String energyAmount;
        @JsonFormat(pattern = "yyyy-MM-DD HH:mm:ss", timezone="Asia/Dhaka")
        public Date orderTime;
    }

    //Used for Amount validation API and Payment API
    public String vendingAmount;
    public String energyAmount;
    public String feeAmount;
    public String phone;
    @JacksonXmlProperty(localName = "fee")
    public Fee fee;

    public static class Fee {
        @JacksonXmlProperty(localName = "item")
        @JacksonXmlElementWrapper(useWrapping = false)
        public List<Item> items;

    }
    public static class Item {
        @JacksonXmlProperty(isAttribute = true)
        public String name;
        @JacksonXmlProperty(isAttribute = true)
        public String amt;
    }

    //Used for Payment API
    public String seq;
    public String token;
    @JsonFormat(pattern = "yyyy-MM-DD HH:mm:ss", timezone="Asia/Dhaka")
    public Date transTime;

    //Used for ack API
    public String orderId;
    public String meter;
    public String amount;
    public String tokens;
    @JsonFormat(pattern = "HH:mm:ss DD-MM-yyyy", timezone="Asia/Dhaka")
    public Date timeStamp;
}
