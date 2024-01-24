package com.grameenphone.wipro.fmfs.mfs_communicator.model.sms;

public class SmsInfo {
    private String msgTransactionId;
    private String language;
    private String senderId;
    private String message;
    private String msgType;
    private String validity;
    private String deliveryReport;

    public String getMsgTransactionId() {
        return msgTransactionId;
    }

    public void setMsgTransactionId(String msgTransactionId) {
        this.msgTransactionId = msgTransactionId;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public String getValidity() {
        return validity;
    }

    public void setValidity(String validity) {
        this.validity = validity;
    }

    public String getDeliveryReport() {
        return deliveryReport;
    }

    public void setDeliveryReport(String deliveryReport) {
        this.deliveryReport = deliveryReport;
    }
}
