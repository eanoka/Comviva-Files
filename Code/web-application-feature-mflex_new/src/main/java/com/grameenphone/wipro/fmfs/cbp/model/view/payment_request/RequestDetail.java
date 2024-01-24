package com.grameenphone.wipro.fmfs.cbp.model.view.payment_request;

import java.util.Date;

public class RequestDetail {
    public long id;
    public long billCount;
    public Date creationTime;
    public String initiator;
    public double amount;
    public String status;
    public String attachment;

    public RequestDetail(long id, long billCount, Date creationTime, String initiator, Double amount, String status,String attachment) {
        this.id = id;
        this.billCount = billCount;
        this.creationTime = creationTime;
        this.initiator = initiator;
        this.amount = amount == null ? 0 : amount;
        this.status = status;
        this.attachment = attachment;
    }
}