package com.grameenphone.wipro.fmfs.mfs_communicator.service.proto;

import com.grameenphone.wipro.exception.ValidationException;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.ConsumerValidateRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.ConsumerValidationResponse.ConsumerValidationResult;

public interface ConsumerValidator {

    ConsumerValidationResult validateConsumer(ConsumerValidateRequest request) throws ValidationException;
}
