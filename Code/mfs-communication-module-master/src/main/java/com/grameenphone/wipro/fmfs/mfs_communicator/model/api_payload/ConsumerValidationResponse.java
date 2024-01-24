package com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload;

import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.ConsumerValidationResponse.ConsumerValidationResult;

import java.util.Map;

public class ConsumerValidationResponse extends BaseResponse<ConsumerValidationResult> {

    public static class ConsumerValidationResult {
        public boolean valid;
        public Map<String, Object> data;
    }
    {
        response = new ConsumerValidationResult();
    }
}
