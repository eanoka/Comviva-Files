package com.grameenphone.wipro.fmfs.mfs_communicator.service.proto;

import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.AmountValidationRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.AmountValidationResponse.AmountValidationResult;

public interface AmountValidator {
    AmountValidationResult validateAmount(AmountValidationRequest request);
}
