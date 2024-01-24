package com.grameenphone.wipro.fmfs.mfs_communicator.model.state;

import com.grameenphone.wipro.fmfs.mfs_communicator.model.mfs_payload.MfsResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.BillPayServiceStatus;

import java.util.UUID;

public class PaymentState {
    public String sessionId = UUID.randomUUID().toString();
    public Throwable exception;
    public MfsResponse mfsPaymentResponse;
    public BillPayServiceStatus billPayServiceStatus;
    public double serviceCharge;
    public double paidAmount;
}