package com.grameenphone.wipro.fmfs.mfs_communicator.model.post_paid_due_bills;

import com.grameenphone.wipro.fmfs.mfs_communicator.model.post_paid_due_bills.paybill_sub_requests.DeviceInfo;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.post_paid_due_bills.paybill_sub_requests.PartnerData;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.post_paid_due_bills.paybill_sub_requests.Receiver;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.post_paid_due_bills.paybill_sub_requests.Sender;
import lombok.Data;

@Data
public class PayBillRequest{
    public String bearerCode;
    public int currency;
    public DeviceInfo deviceInfo;
    public String initiator;
    public String language;
    public PartnerData partnerData;
    public Receiver receiver;
    public String remarks;
    public Sender sender;
    public String serviceFlowId;
    public String externalReferenceId;
}

