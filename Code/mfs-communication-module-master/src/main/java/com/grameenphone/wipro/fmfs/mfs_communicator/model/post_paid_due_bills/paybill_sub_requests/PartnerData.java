package com.grameenphone.wipro.fmfs.mfs_communicator.model.post_paid_due_bills.paybill_sub_requests;

import lombok.Data;

@Data
public class PartnerData {
    public String billAccountNumber;
    public String consumer_id;
    public String billNumber;
    public String surcharge;
    public String vat;
    public String other1;
    public String other2;
    public String billerName;
    public String billerCode;
}
