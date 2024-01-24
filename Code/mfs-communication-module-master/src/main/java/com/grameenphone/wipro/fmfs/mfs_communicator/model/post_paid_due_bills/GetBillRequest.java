package com.grameenphone.wipro.fmfs.mfs_communicator.model.post_paid_due_bills;

import lombok.Data;

import java.util.Map;
@Data
public class GetBillRequest {

    private String utility;
    private String consumer_id;
    private String thirdParty;
    private Map params;

}
