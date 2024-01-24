package com.grameenphone.wipro.fmfs.mfs_communicator.model.post_paid_due_bills;

import lombok.Data;

import java.util.HashMap;
@Data
public class DueBillRequest {

    private String consumer_id;
    private String serviceFlowId;
    private String interfaceId;
    private String serviceType;
    private HashMap<String, Object> params;
    private String utility;

}
