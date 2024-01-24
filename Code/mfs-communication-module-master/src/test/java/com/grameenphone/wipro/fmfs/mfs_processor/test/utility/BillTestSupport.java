package com.grameenphone.wipro.fmfs.mfs_processor.test.utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.grameenphone.wipro.enums.Channel;
import com.grameenphone.wipro.enums.WalletType;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.DueBillsRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentRequest;
import com.grameenphone.wipro.utility.marshal.Json;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Map;

public class BillTestSupport extends MfsMockSupport {
    protected String getDueBillRequest(String company, String consumerId) throws JsonProcessingException {
        DueBillsRequest request = new DueBillsRequest();
        request.msisdn = subscriberMsisdn;
        request.channel = Channel.CBP;
        request.setCompany(company);
        request.setConsumerId(consumerId);
        return Json.toJson(request);
    }

    protected String getPayBillRequest(String company, String consumerId, boolean isSubscriber, Map<String, Object> params) throws JsonProcessingException {
        PaymentRequest request = new PaymentRequest();
        request.msisdn = isSubscriber ? subscriberMsisdn : retailerMsisdn;
        request.channel = Channel.CBP;
        request.setCompany(company);
        request.setConsumerId(consumerId);
        request.amount = billAmount;
        request.bill = RandomStringUtils.randomAlphanumeric(20);
        request.wallet_type = isSubscriber ? WalletType.SUB : WalletType.RET;
        request.customer = isSubscriber ? null : subscriberMsisdn;
        request.params = params;
        request.pin = isSubscriber ? subscriberWalletPin : retailerWalletPin;
        return Json.toJson(request);
    }
}