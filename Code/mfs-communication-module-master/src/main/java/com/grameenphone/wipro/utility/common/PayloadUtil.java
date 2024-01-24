package com.grameenphone.wipro.utility.common;

import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.BaseResponse;

public class PayloadUtil {
    public static <T extends BaseResponse<Y>, Y> T wrapResponse(T target, Y response) {
        target.response = response;
        return target;
    }
}