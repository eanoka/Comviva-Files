package com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload;

import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.ServiceChargeResponse.ServiceChargeResult;

public class ServiceChargeResponse extends BaseResponse<ServiceChargeResult> {
    public static class ServiceChargeResult {
        public double service_charge;
    }

    {
        response = new ServiceChargeResult();
    }
}