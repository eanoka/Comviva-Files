package com.grameenphone.wipro.fmfs.mfs_communicator.model.post_paid_due_bills;

import lombok.Data;

@Data
public class DueBillResponse {
    private String message;
    private int code;
    private String utility;
    private String account_no;
    private long timestamp;
    private BillList[] bill_list;
}
