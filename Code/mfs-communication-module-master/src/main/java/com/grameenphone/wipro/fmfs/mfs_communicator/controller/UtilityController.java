package com.grameenphone.wipro.fmfs.mfs_communicator.controller;

import com.grameenphone.wipro.exception.HttpErrorResponseException;
import com.grameenphone.wipro.exception.ValidationException;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.ConsumerValidateRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.UtilityBalanceResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.UtilityBalanceResponse.UtilityBalanceResult;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.UtilityBalanceChecker;
import com.grameenphone.wipro.utility.common.PayloadUtil;
import com.grameenphone.wipro.utility.common.StringUtil;
import com.grameenphone.wipro.utility.spring.ContextUtil;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/utility")
public class UtilityController {
    @RequestMapping("/balance")
    public UtilityBalanceResponse checkUtilityBalance(@RequestBody ConsumerValidateRequest request) throws HttpErrorResponseException {
        if(StringUtil.isNullOrEmpty(request.consumerId)){
            throw new ValidationException("Consumer Id can not be empty");
        }
        if(StringUtil.isNullOrEmpty(request.company)){
            throw new ValidationException("Company can not be empty");
        }
        UtilityBalanceChecker checker = ContextUtil.getBean(request.company + "_Check_Balance", UtilityBalanceChecker.class);
        if(checker == null) {
            throw new ValidationException(request.company + " is not a valid or supported company");
        }
        UtilityBalanceResult result = checker.checkBalance(request);
        return PayloadUtil.wrapResponse(new UtilityBalanceResponse(), result);
    }
}