package com.grameenphone.wipro.fmfs.mfs_communicator.model.post_paid_due_bills;

import lombok.Data;

@Data
public class BillList {
    private String bill_number;
    private String due_date;
    private double amount;
    private double service_charge;
    private Details detail;
}
