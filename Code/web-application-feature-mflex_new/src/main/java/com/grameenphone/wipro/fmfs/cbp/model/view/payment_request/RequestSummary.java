package com.grameenphone.wipro.fmfs.cbp.model.view.payment_request;

import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Client;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.PaymentRequestHop;

import java.util.Date;

public class RequestSummary {
    public long id;
    public String requester;
    public String requesterEmail;
    public Date date;
    public double payableAmount;
    public double billAmount;
    public double vat;
    public double serviceCharge;
    public Client account;
    public String attachment;
    public PaymentRequestHop lastHop;

    public RequestSummary(long id, String requester, Date date, double billAmount, Double vat, Double serviceCharge, Client account,String attachment) {
        this(id, requester, date, billAmount, vat, serviceCharge, account, attachment, null, null);
    }
    
    public RequestSummary(long id, String requester, Date date, double billAmount, Double vat, Double serviceCharge, Client account,String attachment, PaymentRequestHop lastHop, String email) {
        this.id = id;
        this.requester = requester;
        this.date = date;
        this.billAmount = billAmount;
        this.vat = vat == null ? 0 : vat;
        this.serviceCharge = serviceCharge == null ? 0 : serviceCharge;
        this.payableAmount = billAmount + this.serviceCharge;
        this.account = account;
        this.attachment = attachment;
        this.lastHop = lastHop;
        this.requesterEmail = email;
    }
}