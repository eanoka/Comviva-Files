package com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload;

import java.util.Map;

public class AmountValidationRequest extends ConsumerValidateRequest {
    public Map<String, Object> params;
    public String wallet_type;
    public String channel;
    public String msisdn;
    public double amount;
}