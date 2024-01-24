package com.grameenphone.wipro.fmfs.mfs_communicator.model.post_paid_due_bills.paybill_sub_requests;

import lombok.Data;

import java.util.ArrayList;

@Data
public class Sender {
    public String idType;
    public String idValue;
    public int mpin;
    public ArrayList<PaymentInstrument> paymentInstruments;
    public String userRole;
}
