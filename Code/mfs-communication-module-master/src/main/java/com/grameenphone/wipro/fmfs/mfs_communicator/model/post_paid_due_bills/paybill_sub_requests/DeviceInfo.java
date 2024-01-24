package com.grameenphone.wipro.fmfs.mfs_communicator.model.post_paid_due_bills.paybill_sub_requests;

import lombok.Data;

@Data
public class DeviceInfo {
    public double appVersion;
    public long deviceId;
    public double lattitude;
    public double logitude;
    public String mac;
    public String model;
    public String networkOperator;
    public String networkType;
    public String os;
    public String providerIpAddress;
}
