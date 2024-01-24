package com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload;

import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.UtilityBalanceResponse.UtilityBalanceResult;

public class UtilityBalanceResponse extends BaseResponse<UtilityBalanceResult> {

    public static class UtilityBalanceResult {
        public double balance;
    }
    {
        response = new UtilityBalanceResult();
    }

}
