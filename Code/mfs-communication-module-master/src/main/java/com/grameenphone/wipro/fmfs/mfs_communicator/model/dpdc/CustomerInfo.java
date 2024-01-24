package com.grameenphone.wipro.fmfs.mfs_communicator.model.dpdc;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "CustomerInfo")
public class CustomerInfo {
    @JacksonXmlProperty(localName = "CustomerName")
    private String customerName;

    @JacksonXmlProperty(localName = "TariffProgram")
    private String tariffProgram;

    @JacksonXmlProperty(localName = "CustomerNo")
    private String customerNo;

    @JacksonXmlProperty(localName = "MeterNo")
    private String meterNo;

    @JacksonXmlProperty(localName = "MeterType")
    private String meterType;

    @JacksonXmlProperty(localName = "ContactPerson")
    private String contactPerson;

    @JacksonXmlProperty(localName = "Mobile")
    private String mobile;

    @JacksonXmlProperty(localName = "Dues")
    private String dues;

    @JacksonXmlProperty(localName = "AmountValidationFlag")
    private int amountValidationFlag;

    @JacksonXmlProperty(localName = "msg")
    private String message;
    
    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerNo() {
        return customerNo;
    }

    public void setCustomerNo(String customerNo) {
        this.customerNo = customerNo;
    }

    public String getMeterNo() {
        return meterNo;
    }

    public void setMeterNo(String meterNo) {
        this.meterNo = meterNo;
    }

    public String getMeterType() {
        return meterType;
    }

    public void setMeterType(String meterType) {
        this.meterType = meterType;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getDues() {
        return dues;
    }

    public void setDues(String dues) {
        this.dues = dues;
    }

    public int getAmountValidationFlag() {
        return amountValidationFlag;
    }

    public void setAmountValidationFlag(int amountValidationFlag) {
        this.amountValidationFlag = amountValidationFlag;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTariffProgram() {
        return tariffProgram;
    }

    public void setTariffProgram(String tariffProgram) {
        this.tariffProgram = tariffProgram;
    }
}