package com.grameenphone.wipro.fmfs.mfs_communicator.model.desco;

import java.util.Map;

public class DescoTopUpBreakDownResponse {
    public String transactionId;
    public String accountNo;
    public String meterNo;
    public double revenue;
    public double vat;
    public double amount;
    public double energyCost;
    public Map charges;

    public String message;
    public String token;
    public String responseCode;
    public String customerName;
}
