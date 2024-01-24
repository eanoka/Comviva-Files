package com.grameenphone.wipro.fmfs.mfs_communicator.model.post_paid_due_bills.paybill_sub_requests;

import lombok.Data;

@Data
public class Receiver {
    public String idType;
    public String idValue;
    public int productId;
}
