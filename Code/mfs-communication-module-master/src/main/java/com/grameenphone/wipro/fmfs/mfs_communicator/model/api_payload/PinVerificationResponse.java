package com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload;

public class PinVerificationResponse extends BaseResponse<PinVerificationResponse.Result> {
    public static class Result { 
        public boolean valid;
    }

    {
        response = new Result();
    }
}