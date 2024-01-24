package com.grameenphone.wipro.fmfs.mfs_communicator.model.post_paid_due_bills.paybill_sub_requests;

import lombok.Data;

@Data
public class PayBillCallBackRequest {
    private String mobiquityTxnID;
    private String mobiquityStatus;
    private String mobiquityMessage;
    private String TPTxnID;
    private String TPStatus;
    private String TPMessage;
    private String orderID;

}
