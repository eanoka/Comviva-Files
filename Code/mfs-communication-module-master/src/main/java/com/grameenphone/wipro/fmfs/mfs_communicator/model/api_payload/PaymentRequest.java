package com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload;

import java.util.Map;

public class PaymentRequest extends BillerRequest {
    public String customer;
    public String pin;
    public double amount;
    public String bill;
    public String initiator; // optional. information about channel operator
    public Map<String, Object> params;
    public boolean amount_pre_validated = false; // optional. Used for Other channel where validation already done
    public boolean consumer_pre_validated = false; // optional. Used for Other channel where validation already done

    public double vat;
    public double surviceCharge;
    public String companyMsisdn;
    public int clientDivisionId;
    public int companyId;
    public String companyName;
}