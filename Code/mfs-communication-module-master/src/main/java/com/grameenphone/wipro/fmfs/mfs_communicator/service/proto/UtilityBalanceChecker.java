package com.grameenphone.wipro.fmfs.mfs_communicator.service.proto;

import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.ConsumerValidateRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.UtilityBalanceResponse.UtilityBalanceResult;

public interface UtilityBalanceChecker {
    UtilityBalanceResult checkBalance(ConsumerValidateRequest request);
}