package com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload;

import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentResponse.PaymentResult;

public class PaymentResponse extends BaseResponse<PaymentResult> {

    public static class PaymentResult {
        public String txnId;
        public String vendorTxnId;
        public String status;
        public String message;
        public double serviceCharge;
        public double paidAmount;
        public String failCode;
    }

    {
        response = new PaymentResult();
    }
}