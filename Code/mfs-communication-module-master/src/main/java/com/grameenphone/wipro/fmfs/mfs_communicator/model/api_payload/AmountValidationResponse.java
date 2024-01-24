package com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload;

import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.AmountValidationResponse.AmountValidationResult;

import java.util.Map;

public class AmountValidationResponse extends BaseResponse<AmountValidationResult> {

    public static class AmountValidationResult {
        public boolean valid;
        public String message;
        public double service_charge;
        public Map<String, Object> data;
    }
    {
        response = new AmountValidationResult();
    }
}
